/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.platform;

import static eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType.JAVA_SERVER;
import static eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType.PE_SERVER;

import com.google.common.collect.MapMaker;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf.Mutable;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper.ServiceInfoState;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.AbstractSignManagement;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.util.LayoutUtil;
import eu.cloudnetservice.modules.signs.util.PriorityUtil;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractPlatformSignManagement<T> extends PlatformSignManagement<T> implements SignManagement {

  public static final String REQUEST_CONFIG = "signs_request_config";
  public static final String SET_SIGN_CONFIG = "signs_update_sign_config";
  public static final String SIGN_CREATE = "signs_sign_create";
  public static final String SIGN_DELETE = "signs_sign_delete";
  public static final String SIGN_ALL_DELETE = "signs_sign_delete_all";
  public static final String SIGN_BULK_DELETE = "signs_sign_bulk_delete";

  protected static final int TPS = 20;
  protected static final Sign[] EMPTY_SIGN_ARRAY = new Sign[0];
  protected static final Logger LOGGER = LogManager.logger(AbstractPlatformSignManagement.class);

  protected final Lock updatingLock = new ReentrantLock();
  protected final AtomicInteger currentTick = new AtomicInteger();
  protected final Queue<ServiceInfoSnapshot> waitingAssignments = new ConcurrentLinkedQueue<>();

  protected AbstractPlatformSignManagement() {
    super(loadSignsConfiguration());
  }

  protected static @Nullable SignsConfiguration loadSignsConfiguration() {
    var response = ChannelMessage.builder()
      .channel(AbstractSignManagement.SIGN_CHANNEL_NAME)
      .message(REQUEST_CONFIG)
      .targetNode(Wrapper.instance().nodeUniqueId())
      .build()
      .sendSingleQuery();
    return response == null ? null : response.content().readObject(SignsConfiguration.class);
  }

  @Override
  public void createSign(@NonNull Sign sign) {
    this.channelMessage(SIGN_CREATE)
      .buffer(DataBuf.empty().writeObject(sign))
      .build().send();
  }

  @Override
  public void deleteSign(@NonNull WorldPosition position) {
    this.channelMessage(SIGN_DELETE)
      .buffer(DataBuf.empty().writeObject(position))
      .build().send();
  }

  @Override
  public int deleteAllSigns(@NonNull String group, @Nullable String templatePath) {
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
  public void handleServiceAdd(@NonNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      this.tryAssign(snapshot);
    }
  }

  @Override
  public void handleServiceUpdate(@NonNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      var handlingSign = this.signOf(snapshot);
      if (handlingSign == null) {
        handlingSign = this.nextFreeSign(snapshot);
        // in all cases we need to remove the old waiting assignment
        this.waitingAssignments.removeIf(s -> s.name().equals(snapshot.name()));
        if (handlingSign == null) {
          this.waitingAssignments.add(snapshot);
          return;
        }
      }

      handlingSign.currentTarget(snapshot);
      this.updateSign(handlingSign);
    }
  }

  @Override
  public void handleServiceRemove(@NonNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      var handlingSign = this.signOf(snapshot);
      if (handlingSign != null) {
        handlingSign.currentTarget(null);
        this.updateSign(handlingSign);
      } else {
        this.waitingAssignments.removeIf(s -> s.name().equals(snapshot.name()));
      }
    }
  }

  @Override
  public boolean canConnect(@NonNull Sign sign, @NonNull Function<String, Boolean> permissionChecker) {
    var target = sign.currentTarget();
    if (target == null) {
      return false;
    } else {
      var state = BridgeServiceHelper.guessStateFromServiceInfoSnapshot(target);
      return state == ServiceInfoState.EMPTY_ONLINE
        || state == ServiceInfoState.ONLINE
        || state == ServiceInfoState.FULL_ONLINE;
    }
  }

  @Override
  public void signsConfiguration(@NonNull SignsConfiguration signsConfiguration) {
    this.channelMessage(SET_SIGN_CONFIG)
      .buffer(DataBuf.empty().writeObject(signsConfiguration))
      .build().send();
  }

  @Override
  public void initialize() {
    this.initialize(new MapMaker().weakKeys().makeMap());
  }

  @Override
  public void initialize(@NonNull Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking) {
    if (this.signsConfiguration != null) {
      CloudNetDriver.instance().taskExecutor().scheduleAtFixedRate(() -> {
        try {
          this.tick(signsNeedingTicking);
        } catch (Throwable throwable) {
          LOGGER.severe("Exception ticking signs");
        }
      }, 0, 1000 / TPS, TimeUnit.MILLISECONDS);
      this.startKnockbackTask();

      CloudNetDriver.instance().cloudServiceProvider().servicesAsync().thenAccept(services -> {
        for (var service : services) {
          this.handleServiceAdd(service);
        }
      });
    }
  }

  @Override
  public void handleInternalSignRemove(@NonNull WorldPosition position) {
    if (Wrapper.instance().serviceConfiguration().groups().contains(position.group())) {
      var sign = this.signAt(position);
      if (sign != null && sign.currentTarget() != null) {
        this.waitingAssignments.add(sign.currentTarget());
      }

      super.handleInternalSignRemove(position);
    }
  }

  protected @NonNull String[] replaceLines(@NonNull Sign sign, @NonNull SignLayout layout) {
    var lines = layout.lines();
    if (lines.length == 4) {
      var replacedLines = new String[4];
      for (var i = 0; i < 4; i++) {
        replacedLines[i] = BridgeServiceHelper.fillCommonPlaceholders(
          lines[i],
          sign.targetGroup(),
          sign.currentTarget());
      }
      return replacedLines;
    }
    return null;
  }

  protected boolean shouldAssign(@NonNull ServiceInfoSnapshot snapshot) {
    var currentEnv = Wrapper.instance().serviceId().environment();
    var serviceEnv = snapshot.serviceId().environment();

    return (JAVA_SERVER.get(currentEnv.properties()) && JAVA_SERVER.get(serviceEnv.properties()))
      || PE_SERVER.get(currentEnv.properties()) && PE_SERVER.get(serviceEnv.properties());
  }

  protected void tryAssign(@NonNull ServiceInfoSnapshot snapshot) {
    // check if the service is already assigned to a sign
    var sign = this.signOf(snapshot);
    if (sign == null) {
      // check if there is a free sign to handle the service
      sign = this.nextFreeSign(snapshot);
      if (sign == null) {
        // no free sign, add to the waiting services
        this.waitingAssignments.add(snapshot);
        return;
      }
    }
    // assign the service to the sign and update
    sign.currentTarget(snapshot);
    this.updateSign(sign);
  }

  protected boolean checkTemplatePath(@NonNull ServiceInfoSnapshot snapshot, @NonNull Sign sign) {
    for (var template : snapshot.configuration().templates()) {
      if (template.toString().equals(sign.templatePath())) {
        return true;
      }
    }
    return false;
  }

  protected void updateSign(@NonNull Sign sign) {
    var ownEntry = this.applicableSignConfigurationEntry();
    if (ownEntry != null) {
      this.pushUpdate(sign, LayoutUtil.layout(ownEntry, sign, sign.currentTarget()));
    } else {
      sign.currentTarget(null);
    }
  }

  @Internal
  protected void tick(@NonNull Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking) {
    this.currentTick.incrementAndGet();

    var ownEntry = this.applicableSignConfigurationEntry();
    if (ownEntry != null) {
      for (var value : this.signs.values()) {
        // tick all sign layouts which we need to tick in the current tick
        var holder = LayoutUtil.layoutHolder(ownEntry, value, value.currentTarget());
        if (holder.hasLayouts() && holder.animationsPerSecond() > 0
          && (this.currentTick.get() % 20) % Math.round(20D / holder.animationsPerSecond()) == 0) {
          holder.tick().enableTickBlock();
          signsNeedingTicking.computeIfAbsent(holder, $ -> new HashSet<>()).add(value);
        }
      }

      for (var entry : signsNeedingTicking.entrySet()) {
        // push out all sign changes we recorded previously
        // we need to copy all entries of the set into a new array in case we have a thread de-sync (for example async
        // tick but sync update) as we need to clear the underlying set after the call to prevent double ticks
        this.pushUpdates(entry.getValue().toArray(EMPTY_SIGN_ARRAY), entry.getKey().releaseTickBlock().currentLayout());
        entry.getValue().clear();
      }

      // check if we have waiting services which are not yet assigned - try to assign them to a sign
      if (!this.waitingAssignments.isEmpty()) {
        for (var waitingAssignment : this.waitingAssignments) {
          // get the next free sign to which can assign the service
          var freeSign = this.nextFreeSign(waitingAssignment);
          if (freeSign != null) {
            // remove instantly
            this.waitingAssignments.remove(waitingAssignment);
            // assign the service to the sign and update it
            freeSign.currentTarget(waitingAssignment);
            this.updateSign(freeSign);
          }
        }
      }
    }
  }

  protected @Nullable Sign nextFreeSign(@NonNull ServiceInfoSnapshot snapshot) {
    var entry = this.applicableSignConfigurationEntry();
    var servicePriority = PriorityUtil.priority(snapshot, entry);

    // ensure that we only assign the snapshot to a sign that has no target yet
    this.updatingLock.lock();
    try {
      Sign bestChoice = null;
      for (var sign : this.signs.values()) {
        if (snapshot.configuration().groups().contains(sign.targetGroup())
          && (sign.templatePath() == null || this.checkTemplatePath(snapshot, sign))) {
          // the service could be assigned to the sign
          if (sign.currentTarget() == null) {
            // the sign has no target yet, best choice
            bestChoice = sign;
            break;
          } else {
            // get the priority of the sign depending on the current sign choice (if any)
            var signPriority = sign.priority(entry);
            var priority = bestChoice == null ? servicePriority : bestChoice.priority(entry);
            // check if the service/sign we found has a higher priority than the sign
            if (priority > signPriority) {
              // yes it has, use the sign with the lower priority
              bestChoice = sign;
            } else if (priority == signPriority && bestChoice != null) {
              // no it has the same priority as the best choice
              // check if we get a better template path match than the current selected sign
              if (bestChoice.templatePath() == null && sign.templatePath() != null) {
                // yes the sign has a better template path match than the previous choice
                bestChoice = sign;
              }
            }
          }
        }
      }

      if (bestChoice != null && bestChoice.currentTarget() != null) {
        // enqueue and reset the current target of the sign
        this.waitingAssignments.add(bestChoice.currentTarget());
        bestChoice.currentTarget(null);
      }

      return bestChoice;
    } finally {
      this.updatingLock.unlock();
    }
  }

  protected @Nullable Sign signOf(@NonNull ServiceInfoSnapshot snapshot) {
    for (var value : this.signs.values()) {
      var target = value.currentTarget();
      if (target != null && target.name().equals(snapshot.name())) {
        return value;
      }
    }
    return null;
  }

  protected abstract void pushUpdates(@NonNull Sign[] signs, @NonNull SignLayout layout);

  protected abstract void pushUpdate(@NonNull Sign sign, @NonNull SignLayout layout);
}
