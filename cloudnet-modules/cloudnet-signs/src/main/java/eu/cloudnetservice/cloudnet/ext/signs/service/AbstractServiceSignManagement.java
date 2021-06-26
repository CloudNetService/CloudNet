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

package eu.cloudnetservice.cloudnet.ext.signs.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.util.LayoutUtil;
import eu.cloudnetservice.cloudnet.ext.signs.util.PriorityUtil;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractServiceSignManagement<T> extends ServiceSignManagement<T> implements SignManagement {

  public static final String REQUEST_CONFIG = "signs_request_config";
  public static final String SET_SIGN_CONFIG = "signs_update_sign_config";
  public static final String SIGN_CREATE = "signs_sign_create";
  public static final String SIGN_DELETE = "signs_sign_delete";
  public static final String SIGN_ALL_DELETE = "signs_sign_delete_all";
  public static final String SIGN_BULK_DELETE = "signs_sign_bulk_delete";

  protected static final int TPS = 20;

  protected final AtomicInteger currentTick = new AtomicInteger();
  protected final Queue<ServiceInfoSnapshot> waitingAssignments = new ConcurrentLinkedQueue<>();

  protected AbstractServiceSignManagement() {
    super(loadSignsConfiguration());
  }

  protected static @Nullable SignsConfiguration loadSignsConfiguration() {
    ChannelMessage response = ChannelMessage.builder()
      .channel(SIGN_CHANNEL_NAME)
      .message(REQUEST_CONFIG)
      .targetNode(Wrapper.getInstance().getNodeUniqueId())
      .build()
      .sendSingleQuery();
    return response == null ? null : response.getBuffer().readObject(SignsConfiguration.class);
  }

  @Override
  public void createSign(@NotNull Sign sign) {
    this.channelMessage(SIGN_CREATE)
      .buffer(ProtocolBuffer.create().writeObject(sign))
      .build().send();
  }

  @Override
  public void deleteSign(@NotNull WorldPosition position) {
    this.channelMessage(SIGN_DELETE)
      .buffer(ProtocolBuffer.create().writeObject(position))
      .build().send();
  }

  @Override
  public int deleteAllSigns(@NotNull String group, @Nullable String templatePath) {
    ChannelMessage response = this.channelMessage(SIGN_BULK_DELETE)
      .buffer(ProtocolBuffer.create().writeString(group).writeOptionalString(templatePath))
      .build().sendSingleQuery();
    return response == null ? 0 : response.getBuffer().readVarInt();
  }

  @Override
  public int deleteAllSigns() {
    this.channelMessage(SIGN_ALL_DELETE)
      .buffer(ProtocolBuffer.create().writeObjectCollection(this.signs.keySet()))
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
      Sign handlingSign = this.getSignOf(snapshot);
      if (handlingSign == null) {
        handlingSign = this.getNextFreeSign(snapshot);
        // in all cases we need to remove the old waiting assignment
        this.waitingAssignments.removeIf(s -> s.getName().equals(snapshot.getName()));
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
      Sign handlingSign = this.getSignOf(snapshot);
      if (handlingSign != null) {
        handlingSign.setCurrentTarget(null);
        this.updateSign(handlingSign);
      } else {
        this.waitingAssignments.removeIf(s -> s.getName().equals(snapshot.getName()));
      }
    }
  }

  @Override
  public boolean canConnect(@NotNull Sign sign, @NotNull Function<String, Boolean> permissionChecker) {
    if (sign.getCurrentTarget() == null) {
      return false;
    } else {
      ServiceInfoStateWatcher.ServiceInfoState state = ServiceInfoStateWatcher
        .stateFromServiceInfoSnapshot(sign.getCurrentTarget());
      return state == ServiceInfoStateWatcher.ServiceInfoState.EMPTY_ONLINE
        || state == ServiceInfoStateWatcher.ServiceInfoState.ONLINE
        || state == ServiceInfoStateWatcher.ServiceInfoState.FULL_ONLINE;
    }
  }

  @Override
  public void setSignsConfiguration(@NotNull SignsConfiguration signsConfiguration) {
    this.channelMessage(SET_SIGN_CONFIG)
      .buffer(ProtocolBuffer.create().writeObject(signsConfiguration))
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
          System.err.println("Exception ticking signs");
          throwable.printStackTrace();
        }
      }, 0, 1000 / TPS, TimeUnit.MILLISECONDS);
      this.startKnockbackTask();

      CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesAsync().onComplete(services -> {
        for (ServiceInfoSnapshot service : services) {
          this.handleServiceAdd(service);
        }
      });
    }
  }

  @Override
  public void handleInternalSignRemove(@NotNull WorldPosition position) {
    if (Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(position.getGroup())) {
      Sign sign = this.getSignAt(position);
      if (sign != null && sign.getCurrentTarget() != null) {
        this.waitingAssignments.add(sign.getCurrentTarget());
      }

      super.handleInternalSignRemove(position);
    }
  }

  protected @NotNull String[] replaceLines(@NotNull Sign sign, @NotNull SignLayout layout) {
    String[] lines = layout.getLines();
    if (lines != null && lines.length == 4) {
      String[] replacedLines = new String[4];
      for (int i = 0; i < 4; i++) {
        replacedLines[i] = ChatColor.translateAlternateColorCodes('&',
          ServiceInfoStateWatcher.replaceServiceInfo(lines[i], sign.getTargetGroup(), sign.getCurrentTarget()));
      }
      return replacedLines;
    }
    return null;
  }

  protected boolean shouldAssign(@NotNull ServiceInfoSnapshot snapshot) {
    ServiceEnvironmentType currentEnvironment = Wrapper.getInstance().getServiceId().getEnvironment();
    ServiceEnvironmentType serviceEnvironment = snapshot.getServiceId().getEnvironment();

    return (serviceEnvironment.isMinecraftJavaServer() && currentEnvironment.isMinecraftJavaServer())
      || (serviceEnvironment.isMinecraftBedrockServer() && currentEnvironment.isMinecraftBedrockServer());
  }

  protected void tryAssign(@NotNull ServiceInfoSnapshot snapshot) {
    // check if the service is already assigned to a sign
    Sign sign = this.getSignOf(snapshot);
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
    for (ServiceTemplate template : snapshot.getConfiguration().getTemplates()) {
      if (template.getTemplatePath().equals(sign.getTemplatePath())) {
        return true;
      }
    }
    return false;
  }

  protected void updateSign(@NotNull Sign sign) {
    SignConfigurationEntry ownEntry = this.getApplicableSignConfigurationEntry();
    if (ownEntry != null) {
      this.pushUpdate(sign, LayoutUtil.getLayout(ownEntry, sign, sign.getCurrentTarget()));
    } else {
      sign.setCurrentTarget(null);
    }
  }

  @ApiStatus.Internal
  protected void tick(@NotNull Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking) {
    this.currentTick.incrementAndGet();

    SignConfigurationEntry ownEntry = this.getApplicableSignConfigurationEntry();
    if (ownEntry != null) {
      for (Sign value : this.signs.values()) {
        SignLayoutsHolder holder = LayoutUtil.getLayoutHolder(ownEntry, value, value.getCurrentTarget());
        if (holder.hasLayouts() && holder.getAnimationsPerSecond() > 0
          && (this.currentTick.get() % 20) % Math.round(20D / holder.getAnimationsPerSecond()) == 0) {
          holder.tick().setTickBlocked(true);
          signsNeedingTicking.computeIfAbsent(holder, s -> new HashSet<>()).add(value);
        }
      }

      for (Map.Entry<SignLayoutsHolder, Set<Sign>> entry : signsNeedingTicking.entrySet()) {
        this.pushUpdates(ImmutableSet.copyOf(entry.getValue()), entry.getKey().releaseTickBlock().getCurrentLayout());
        // remove updated sign from the map
        entry.getValue().clear();
      }

      if (!this.waitingAssignments.isEmpty()) {
        for (ServiceInfoSnapshot waitingAssignment : this.waitingAssignments) {
          Sign freeSign = this.getNextFreeSign(waitingAssignment);
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
    SignConfigurationEntry entry = this.getApplicableSignConfigurationEntry();
    int servicePriority = PriorityUtil.getPriority(snapshot, entry);

    synchronized (this) {
      Sign bestChoice = null;
      for (Sign sign : this.signs.values()) {
        if (Arrays.asList(snapshot.getConfiguration().getGroups()).contains(sign.getTargetGroup())
          && (sign.getTemplatePath() == null || this.checkTemplatePath(snapshot, sign))) {
          // the service could be assigned to the sign
          if (sign.getCurrentTarget() == null) {
            // the sign has no target yet, best choice
            bestChoice = sign;
            break;
          } else {
            // get the priority depending if we found a sign yet
            int signPriority = sign.getPriority(entry);
            int priority = bestChoice == null ? servicePriority : bestChoice.getPriority(entry);
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
    for (Sign value : this.signs.values()) {
      if (value.getCurrentTarget() != null && value.getCurrentTarget().getName().equals(snapshot.getName())) {
        return value;
      }
    }
    return null;
  }

  protected abstract void pushUpdates(@NotNull Set<Sign> signs, @NotNull SignLayout layout);

  protected abstract void pushUpdate(@NotNull Sign sign, @NotNull SignLayout layout);
}
