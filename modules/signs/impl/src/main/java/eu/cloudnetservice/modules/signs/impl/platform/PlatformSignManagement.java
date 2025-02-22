/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.impl.platform;

import static eu.cloudnetservice.driver.service.ServiceEnvironmentType.JAVA_SERVER;
import static eu.cloudnetservice.driver.service.ServiceEnvironmentType.PE_SERVER;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.modules.signs.configuration.SignLayoutsHolder;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.AbstractSignManagement;
import eu.cloudnetservice.modules.signs.impl.SharedChannelMessageListener;
import eu.cloudnetservice.modules.signs.impl.util.LayoutUtil;
import eu.cloudnetservice.modules.signs.impl.util.PriorityUtil;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PlatformSignManagement<P, L, C> extends AbstractSignManagement {

  public static final String REQUEST_CONFIG = "signs_request_config";
  public static final String SET_SIGN_CONFIG = "signs_update_sign_config";
  public static final String SIGN_CREATE = "signs_sign_create";
  public static final String SIGN_DELETE = "signs_sign_delete";
  public static final String SIGN_ALL_DELETE = "signs_sign_delete_all";
  public static final String SIGN_BULK_DELETE = "signs_sign_bulk_delete";
  public static final String SIGN_GET_SIGNS_BY_GROUPS = "signs_get_signs_by_groups";

  protected static final Logger LOGGER = LoggerFactory.getLogger(PlatformSignManagement.class);

  protected final Executor mainThreadExecutor;
  protected final WrapperConfiguration wrapperConfig;
  protected final CloudServiceProvider serviceProvider;
  protected final ScheduledExecutorService executorService;

  protected final Lock updatingLock = new ReentrantLock();
  protected final Map<WorldPosition, PlatformSign<P, C>> platformSigns = new ConcurrentHashMap<>();
  protected final Queue<ServiceInfoSnapshot> waitingAssignments = new ConcurrentLinkedQueue<>();

  protected int currentTick;

  protected PlatformSignManagement(
    @NonNull EventManager eventManager,
    @NonNull Executor mainThreadExecutor,
    @NonNull WrapperConfiguration wrapperConfig,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull ScheduledExecutorService executorService
  ) {
    super(loadSignsConfiguration(wrapperConfig));
    this.mainThreadExecutor = mainThreadExecutor;
    this.wrapperConfig = wrapperConfig;
    this.serviceProvider = serviceProvider;
    this.executorService = executorService;
    // get the signs for the current group
    var groups = wrapperConfig.serviceConfiguration().groups();
    for (var sign : this.signs(groups)) {
      this.signs.put(sign.location(), sign);
    }
    // register the listeners
    eventManager.registerListener(SignsPlatformListener.class);
    eventManager.registerListener(SharedChannelMessageListener.class);
  }

  protected static @Nullable SignsConfiguration loadSignsConfiguration(@NonNull WrapperConfiguration wrapperConfig) {
    var response = ChannelMessage.builder()
      .channel(AbstractSignManagement.SIGN_CHANNEL_NAME)
      .message(REQUEST_CONFIG)
      .targetNode(wrapperConfig.serviceConfiguration().serviceId().nodeUniqueId())
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
      .buffer(DataBuf.empty().writeString(group).writeNullable(templatePath, DataBuf.Mutable::writeString))
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
  public @NonNull Collection<Sign> signs(@NonNull Collection<String> groups) {
    var response = this.channelMessage(SIGN_GET_SIGNS_BY_GROUPS)
      .buffer(DataBuf.empty().writeObject(groups))
      .build()
      .sendSingleQuery();
    return response == null ? Set.of() : response.content().readObject(Sign.COLLECTION_TYPE);
  }

  @Override
  public void signsConfiguration(@NonNull SignsConfiguration signsConfiguration) {
    this.channelMessage(SET_SIGN_CONFIG)
      .buffer(DataBuf.empty().writeObject(signsConfiguration))
      .build().send();
  }

  @Override
  public void handleInternalSignCreate(@NonNull Sign sign) {
    if (this.wrapperConfig.serviceConfiguration().groups().contains(sign.location().group())) {
      var newSign = this.createPlatformSign(sign);
      var oldSign = this.platformSigns.remove(sign.location());

      // set the old target in the new sign if needed
      if (oldSign != null) {
        newSign.currentTarget(oldSign.currentTarget());
      }

      // register the sign
      this.platformSigns.put(sign.location(), newSign);
      super.handleInternalSignCreate(sign);
    }
  }

  @Override
  public void handleInternalSignRemove(@NonNull WorldPosition position) {
    if (this.wrapperConfig.serviceConfiguration().groups().contains(position.group())) {
      var sign = this.platformSigns.remove(position);
      if (sign != null && sign.currentTarget() != null) {
        this.waitingAssignments.add(sign.currentTarget());
      }

      super.handleInternalSignRemove(position);
    }
  }

  @Override
  protected @NonNull ChannelMessage.Builder channelMessage(@NonNull String message) {
    return super.channelMessage(message)
      .target(ChannelMessageTarget.Type.NODE, this.wrapperConfig.serviceConfiguration().serviceId().nodeUniqueId());
  }

  public int removeAllMissingSigns() {
    return this.removeMissingSigns(sign -> true);
  }

  public int removeMissingSigns(@NonNull String world) {
    return this.removeMissingSigns(sign -> sign.base().location().world().equalsIgnoreCase(world));
  }

  public int removeMissingSigns(@NonNull Predicate<PlatformSign<P, C>> filter) {
    var removed = 0;
    for (var sign : this.platformSigns.values()) {
      if (filter.test(sign) && !sign.exists()) {
        this.deleteSign(sign.base());
        removed++;
      }
    }

    return removed;
  }

  public void handleServiceAdd(@NonNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      this.tryAssign(snapshot);
    }
  }

  public void handleServiceUpdate(@NonNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      var handlingSign = this.signOf(snapshot);
      if (handlingSign == null) {
        handlingSign = this.nextFreeSign(snapshot);
        // in all cases we need to remove the old waiting assignment
        this.waitingAssignments.removeIf(s -> s.serviceId().uniqueId().equals(snapshot.serviceId().uniqueId()));
        if (handlingSign == null) {
          this.waitingAssignments.add(snapshot);
          return;
        }
      }

      handlingSign.currentTarget(snapshot);
    }
  }

  public void handleServiceRemove(@NonNull ServiceInfoSnapshot snapshot) {
    if (this.shouldAssign(snapshot)) {
      var handlingSign = this.signOf(snapshot);
      if (handlingSign != null) {
        handlingSign.currentTarget(null);
      } else {
        this.waitingAssignments.removeIf(s -> s.serviceId().uniqueId().equals(snapshot.serviceId().uniqueId()));
      }
    }
  }

  public void initialize() {
    this.initialize(new HashMap<>());
  }

  public void initialize(@NonNull Map<SignLayoutsHolder, Set<PlatformSign<P, C>>> signsNeedingTicking) {
    if (this.signsConfiguration != null) {
      // initialize the platform signs
      for (var value : this.signs.values()) {
        this.platformSigns.put(value.location(), this.createPlatformSign(value));
      }

      // start the needed tasks
      this.executorService.scheduleWithFixedDelay(() -> {
        try {
          this.tick(signsNeedingTicking);
        } catch (Throwable throwable) {
          LOGGER.error("Exception ticking signs", throwable);
        }
      }, 0, 1000 / this.tps(), TimeUnit.MILLISECONDS);
      this.startKnockbackTask();

      // load and register all services
      this.serviceProvider.servicesAsync().thenAccept(services -> {
        for (var service : services) {
          this.handleServiceAdd(service);
        }
      });
    }
  }

  public @Nullable SignConfigurationEntry applicableSignConfigurationEntry() {
    for (var entry : this.signsConfiguration.entries()) {
      if (this.wrapperConfig.serviceConfiguration().groups().contains(entry.targetGroup())) {
        return entry;
      }
    }
    return null;
  }

  protected boolean shouldAssign(@NonNull ServiceInfoSnapshot snapshot) {
    var currentEnv = this.wrapperConfig.serviceConfiguration().serviceId().environment();
    var serviceEnv = snapshot.serviceId().environment();

    return (currentEnv.readProperty(JAVA_SERVER) && serviceEnv.readProperty(JAVA_SERVER)
      || currentEnv.readProperty(PE_SERVER) && serviceEnv.readProperty(PE_SERVER));
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
    // assign the service to the sign
    sign.currentTarget(snapshot);
  }

  protected boolean checkTemplatePath(@NonNull ServiceInfoSnapshot snapshot, @NonNull Sign sign) {
    for (var template : snapshot.configuration().templates()) {
      if (template.toString().equals(sign.templatePath())) {
        return true;
      }
    }
    return false;
  }

  @ApiStatus.Internal
  protected void tick(@NonNull Map<SignLayoutsHolder, Set<PlatformSign<P, C>>> signsNeedingTicking) {
    this.currentTick++;

    var ownEntry = this.applicableSignConfigurationEntry();
    if (ownEntry != null) {
      // marker if there are any updates we need to do - if there are no updates there is no need to schedule them
      // which saves server resources
      var hasUpdates = false;
      for (var value : this.platformSigns.values()) {
        // tick all sign layouts which we need to tick in the current tick
        var holder = LayoutUtil.layoutHolder(ownEntry, value.base(), value.currentTarget());
        if (holder.hasLayouts() && holder.animationsPerSecond() > 0
          && this.currentTick % (this.tps() / holder.animationsPerSecond()) == 0) {
          // tick the holder, then block the tick
          holder.tick().enableTickBlock();
          // register the sign for updates if we need to
          if (value.needsUpdates()) {
            hasUpdates = true;
            signsNeedingTicking.computeIfAbsent(holder, $ -> new HashSet<>()).add(value);
          }
        }
      }

      // execute updates if there are any
      if (hasUpdates) {
        this.mainThreadExecutor.execute(() -> {
          for (var entry : signsNeedingTicking.entrySet()) {
            var layout = entry.getKey().releaseTickBlock().currentLayout();
            // push out all sign changes we recorded previously
            // we need to copy all entries of the set into a new array in case we have a thread de-sync (for example async
            // tick but sync update) as we need to clear the underlying set after the call to prevent double ticks
            var iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
              // update the sign, at this point the sign must be loaded - we can just push the change and unregister it
              iterator.next().updateSign(layout);
              iterator.remove();
            }
          }
        });
      }

      // check if we have waiting services which are not yet assigned - try to assign them to a sign
      if (!this.waitingAssignments.isEmpty()) {
        for (var waitingAssignment : this.waitingAssignments) {
          // get the next free sign to which can assign the service
          var freeSign = this.nextFreeSign(waitingAssignment);
          if (freeSign != null) {
            // remove instantly
            this.waitingAssignments.remove(waitingAssignment);
            // assign the service to the sign, the layout of it will be updated within the next second
            // we could directly update the layout but there is no need to do that
            freeSign.currentTarget(waitingAssignment);
          }
        }
      }
    }

    // reset the tick counter if we reached the max tps
    if (this.currentTick >= this.tps()) {
      this.currentTick = 0;
    }
  }

  protected @Nullable PlatformSign<P, C> nextFreeSign(@NonNull ServiceInfoSnapshot snapshot) {
    var entry = this.applicableSignConfigurationEntry();
    var servicePriority = PriorityUtil.priority(snapshot, entry);

    // ensure that we only assign the snapshot to a sign that has no target yet
    this.updatingLock.lock();
    try {
      PlatformSign<P, C> bestChoice = null;
      for (var platformSign : this.platformSigns.values()) {
        if (!platformSign.needsUpdates() || !platformSign.exists()) {
          continue;
        }

        var sign = platformSign.base();
        if (snapshot.configuration().groups().contains(sign.targetGroup())
          && (sign.templatePath() == null || this.checkTemplatePath(snapshot, sign))) {
          // the service could be assigned to the sign
          if (platformSign.currentTarget() == null) {
            // the sign has no target yet, best choice
            bestChoice = platformSign;
            break;
          } else {
            // get the priority of the sign depending on the current sign choice (if any)
            var signPriority = platformSign.priority(entry);
            var priority = bestChoice == null ? servicePriority : bestChoice.priority(entry);
            // check if the service/sign we found has a higher priority than the sign
            if (priority > signPriority) {
              // yes it has, use the sign with the lower priority
              bestChoice = platformSign;
            } else if (priority == signPriority && bestChoice != null) {
              // no it has the same priority as the best choice
              // check if we get a better template path match than the current selected sign
              if (bestChoice.base().templatePath() == null && sign.templatePath() != null) {
                // yes the sign has a better template path match than the previous choice
                bestChoice = platformSign;
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

  protected @Nullable PlatformSign<P, C> signOf(@NonNull ServiceInfoSnapshot snapshot) {
    for (var value : this.platformSigns.values()) {
      var target = value.currentTarget();
      if (target != null && target.name().equals(snapshot.name())) {
        return value;
      }
    }
    return null;
  }

  public @Nullable PlatformSign<P, C> platformSignAt(@Nullable WorldPosition position) {
    return position == null ? null : this.platformSigns.get(position);
  }

  protected abstract int tps();

  protected abstract void startKnockbackTask();

  public abstract @Nullable WorldPosition convertPosition(@NonNull L location);

  protected abstract @NonNull PlatformSign<P, C> createPlatformSign(@NonNull Sign base);
}
