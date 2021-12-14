/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.cloudnet.ext.signs.platform;

import static de.dytanic.cloudnet.driver.service.ServiceEnvironmentType.JAVA_SERVER;
import static de.dytanic.cloudnet.driver.service.ServiceEnvironmentType.PE_SERVER;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf.Mutable;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper.ServiceInfoState;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.util.LayoutUtil;
import eu.cloudnetservice.cloudnet.ext.signs.util.PriorityUtil;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractPlatformSignManagement<T> extends PlatformSignManagement<T> implements SignManagement {

  public static final String REQUEST_CONFIG = "signs_request_config";
  public static final String SET_SIGN_CONFIG = "signs_update_sign_config";
  public static final String SIGN_CREATE = "signs_sign_create";
  public static final String SIGN_DELETE = "signs_sign_delete";
  public static final String SIGN_ALL_DELETE = "signs_sign_delete_all";
  public static final String SIGN_BULK_DELETE = "signs_sign_bulk_delete";
  protected static final int TPS = 20;
  private static final Logger LOGGER = LogManager.getLogger(AbstractPlatformSignManagement.class);
  protected final AtomicInteger currentTick = new AtomicInteger();
  protected final Queue<ServiceInfoSnapshot> waitingAssignments = new ConcurrentLinkedQueue<>();

  protected AbstractPlatformSignManagement() {
    super(loadSignsConfiguration());
  }

  protected static @Nullable SignsConfiguration loadSignsConfiguration() {
    var response = ChannelMessage.builder()
      .channel(SIGN_CHANNEL_NAME)
      .message(REQUEST_CONFIG)
      .targetNode(Wrapper.getInstance().getNodeUniqueId())
      .build()
      .sendSingleQuery();
    return response == null ? null : response.content().readObject(SignsConfiguration.class);
  }

  @Override
  public void createSign(@NotNull Sign sign) {
    this.channelMessage(SIGN_CREATE)
      .buffer(DataBuf.empty().writeObject(sign))
      .build().send();
  }

  @Override
  public void deleteSign(@NotNull WorldPosition position) {
    this.channelMessage(SIGN_DELETE)
      .buffer(DataBuf.empty().writeObject(position))
      .build().send();
  }

  @Override
  public int deleteAllSigns(@NotNull String group, @Nullable String templatePath) {
    var response = this.channelMessage(SIGN_BULK_DELETE)
      .buffer(DataBuf.empty().writeString(group).writeNullable(templatePath, Mutable::writeString))
      .build().sendSingleQuery();
    return response == null ? 0 : response.content().readInt();
  }

  @Override
  public int deleteAllSigns() {
    this.channelMessage(SIGN_ALL_DELETE)
      .buffer(DataBuf.empty().writeObject(this.signs.keySet()))
      .build().send();
    return this.signs.size();
  }

  @Override
  public void handleServiceAdd(@NotNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      this.tryAssign(snapshot);
    }
  }

  @Override
  public void handleServiceUpdate(@NotNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      var handlingSign = this.getSignOf(snapshot);
      if (handlingSign == null) {
        handlingSign = this.getNextFreeSign(snapshot);
        // in all cases we need to remove the old waiting assignment
        this.waitingAssignments.removeIf(s -> s.name().equals(snapshot.name()));
        if (handlingSign == null) {
          this.waitingAssignments.add(snapshot);
          return;
        }
      }

      handlingSign.setCurrentTarget(snapshot);
      this.updateSign(handlingSign);
    }
  }

  @Override
  public void handleServiceRemove(@NotNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      var handlingSign = this.getSignOf(snapshot);
      if (handlingSign != null) {
        handlingSign.setCurrentTarget(null);
        this.updateSign(handlingSign);
      } else {
        this.waitingAssignments.removeIf(s -> s.name().equals(snapshot.name()));
      }
    }
  }

  @Override
  public boolean canConnect(@NotNull Sign sign, @NotNull Function<String, Boolean> permissionChecker) {
    if (sign.getCurrentTarget() == null) {
      return false;
    } else {
      var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(sign.getCurrentTarget());
      return state == ServiceInfoState.EMPTY_ONLINE
        || state == ServiceInfoState.ONLINE
        || state == ServiceInfoState.FULL_ONLINE;
    }
  }

  @Override
  public void setSignsConfiguration(@NotNull SignsConfiguration signsConfiguration) {
    this.channelMessage(SET_SIGN_CONFIG)
      .buffer(DataBuf.empty().writeObject(signsConfiguration))
      .build().send();
  }

  @Override
  public void initialize() {
    this.initialize(new MapMaker().weakKeys().makeMap());
  }

  @Override
  public void initialize(@NotNull Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking) {
    if (this.signsConfiguration != null) {
      CloudNetDriver.getInstance().getTaskExecutor().scheduleAtFixedRate(() -> {
        try {
          this.tick(signsNeedingTicking);
        } catch (Throwable throwable) {
          LOGGER.severe("Exception ticking signs");
        }
      }, 0, 1000 / TPS, TimeUnit.MILLISECONDS);
      this.startKnockbackTask();

      CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync().onComplete(services -> {
        for (var service : services) {
          this.handleServiceAdd(service);
        }
      });
    }
  }

  @Override
  public void handleInternalSignRemove(@NotNull WorldPosition position) {
    if (Wrapper.getInstance().getServiceConfiguration().getGroups().contains(position.group())) {
      var sign = this.getSignAt(position);
      if (sign != null && sign.getCurrentTarget() != null) {
        this.waitingAssignments.add(sign.getCurrentTarget());
      }

      super.handleInternalSignRemove(position);
    }
  }

  protected @NotNull String[] replaceLines(@NotNull Sign sign, @NotNull SignLayout layout) {
    var lines = layout.getLines();
    if (lines != null && lines.length == 4) {
      var replacedLines = new String[4];
      for (var i = 0; i < 4; i++) {
        replacedLines[i] = BridgeServiceHelper.fillCommonPlaceholders(
          lines[i],
          sign.getTargetGroup(),
          sign.getCurrentTarget());
      }
      return replacedLines;
    }
    return null;
  }

  protected boolean shouldAssign(@NotNull ServiceInfoSnapshot snapshot) {
    var currentEnv = Wrapper.getInstance().getServiceId().getEnvironment();
    var serviceEnv = snapshot.getServiceId().getEnvironment();

    return (JAVA_SERVER.get(currentEnv.getProperties()) && JAVA_SERVER.get(serviceEnv.getProperties()))
      || PE_SERVER.get(currentEnv.getProperties()) && PE_SERVER.get(serviceEnv.getProperties());
  }

  protected void tryAssign(@NotNull ServiceInfoSnapshot snapshot) {
    // check if the service is already assigned to a sign
    var sign = this.getSignOf(snapshot);
    if (sign == null) {
      // check if there is a free sign to handle the service
      sign = this.getNextFreeSign(snapshot);
      if (sign == null) {
        // no free sign, add to the waiting services
        this.waitingAssignments.add(snapshot);
        return;
      }
    }
    // assign the service to the sign and update
    sign.setCurrentTarget(snapshot);
    this.updateSign(sign);
  }

  protected boolean checkTemplatePath(@NotNull ServiceInfoSnapshot snapshot, @NotNull Sign sign) {
    for (var template : snapshot.getConfiguration().getTemplates()) {
      if (template.toString().equals(sign.getTemplatePath())) {
        return true;
      }
    }
    return false;
  }

  protected void updateSign(@NotNull Sign sign) {
    var ownEntry = this.getApplicableSignConfigurationEntry();
    if (ownEntry != null) {
      this.pushUpdate(sign, LayoutUtil.getLayout(ownEntry, sign, sign.getCurrentTarget()));
    } else {
      sign.setCurrentTarget(null);
    }
  }

  @Internal
  protected void tick(@NotNull Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking) {
    this.currentTick.incrementAndGet();

    var ownEntry = this.getApplicableSignConfigurationEntry();
    if (ownEntry != null) {
      for (var value : this.signs.values()) {
        var holder = LayoutUtil.getLayoutHolder(ownEntry, value, value.getCurrentTarget());
        if (holder.hasLayouts() && holder.getAnimationsPerSecond() > 0
          && (this.currentTick.get() % 20) % Math.round(20D / holder.getAnimationsPerSecond()) == 0) {
          holder.tick().enableTickBlock();
          signsNeedingTicking.computeIfAbsent(holder, $ -> new HashSet<>()).add(value);
        }
      }

      for (var entry : signsNeedingTicking.entrySet()) {
        this.pushUpdates(ImmutableSet.copyOf(entry.getValue()), entry.getKey().releaseTickBlock().getCurrentLayout());
        // remove updated sign from the map
        entry.getValue().clear();
      }

      if (!this.waitingAssignments.isEmpty()) {
        for (var waitingAssignment : this.waitingAssignments) {
          var freeSign = this.getNextFreeSign(waitingAssignment);
          if (freeSign != null) {
            this.waitingAssignments.remove(waitingAssignment);

            freeSign.setCurrentTarget(waitingAssignment);
            this.updateSign(freeSign);
          }
        }
      }
    }
  }

  protected @Nullable Sign getNextFreeSign(@NotNull ServiceInfoSnapshot snapshot) {
    var entry = this.getApplicableSignConfigurationEntry();
    var servicePriority = PriorityUtil.getPriority(snapshot, entry);

    synchronized (this) {
      Sign bestChoice = null;
      for (var sign : this.signs.values()) {
        if (snapshot.getConfiguration().getGroups().contains(sign.getTargetGroup())
          && (sign.getTemplatePath() == null || this.checkTemplatePath(snapshot, sign))) {
          // the service could be assigned to the sign
          if (sign.getCurrentTarget() == null) {
            // the sign has no target yet, best choice
            bestChoice = sign;
            break;
          } else {
            // get the priority depending if we found a sign yet
            var signPriority = sign.getPriority(entry);
            var priority = bestChoice == null ? servicePriority : bestChoice.getPriority(entry);
            // check if the service/sign we found has a higher priority than the sign
            if (priority > signPriority) {
              // yes it has, use the sign with the lower priority
              bestChoice = sign;
            } else if (priority == signPriority && bestChoice != null) {
              // no it has the same priority as the found best choice, check if we get a better template
              // path match than using the best choice
              if (bestChoice.getTemplatePath() == null && sign.getTemplatePath() != null) {
                // yes the sign has a better template path match than the previous choice
                bestChoice = sign;
              }
            }
          }
        }
      }

      if (bestChoice != null && bestChoice.getCurrentTarget() != null) {
        // enqueue the current target of the sign
        this.waitingAssignments.add(bestChoice.getCurrentTarget());
        // reset the signs target
        bestChoice.setCurrentTarget(null);
      }

      return bestChoice;
    }
  }

  protected @Nullable Sign getSignOf(@NotNull ServiceInfoSnapshot snapshot) {
    for (var value : this.signs.values()) {
      if (value.getCurrentTarget() != null && value.getCurrentTarget().name().equals(snapshot.name())) {
        return value;
      }
    }
    return null;
  }

  protected abstract void pushUpdates(@NotNull Set<Sign> signs, @NotNull SignLayout layout);

  protected abstract void pushUpdate(@NotNull Sign sign, @NotNull SignLayout layout);
}
