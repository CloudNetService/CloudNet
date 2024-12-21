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

package eu.cloudnetservice.modules.signs.platform.minestom;

import com.google.common.util.concurrent.MoreExecutors;
import eu.cloudnetservice.common.tuple.Tuple2;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.bridge.platform.minestom.MinestomBridgeManagement;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.timer.SchedulerManager;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "minestom", types = {PlatformSignManagement.class, SignManagement.class})
public class MinestomSignManagement extends PlatformSignManagement<Player, Tuple2<Point, Instance>, String> {

  private final ServiceRegistry serviceRegistry;
  private final GlobalEventHandler eventHandler;
  private final InstanceManager instanceManager;
  private final SchedulerManager schedulerManager;
  private final MinestomBridgeManagement bridgeManagement;

  @Inject
  protected MinestomSignManagement(
    @NonNull EventManager eventManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull GlobalEventHandler eventHandler,
    @NonNull InstanceManager instanceManager,
    @NonNull SchedulerManager schedulerManager,
    @NonNull WrapperConfiguration wrapperConfig,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull MinestomBridgeManagement bridgeManagement,
    @NonNull @Named("taskScheduler") ScheduledExecutorService executorService
  ) {
    super(eventManager, MoreExecutors.directExecutor(), wrapperConfig, serviceProvider, executorService);

    this.serviceRegistry = serviceRegistry;
    this.eventHandler = eventHandler;
    this.instanceManager = instanceManager;
    this.schedulerManager = schedulerManager;
    this.bridgeManagement = bridgeManagement;
  }

  @Override
  protected int tps() {
    return ServerFlag.SERVER_TICKS_PER_SECOND;
  }

  @Override
  protected void startKnockbackTask() {
    this.schedulerManager.scheduleTask(() -> {
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var conf = entry.knockbackConfiguration();
        if (conf.validAndEnabled()) {
          var distance = conf.distance();
          // find all signs which need to knock back the player
          for (var sign : this.platformSigns.values()) {
            if (sign.needsUpdates() && sign.exists() && sign instanceof MinestomPlatformSign minestomSigns) {
              var location = minestomSigns.signLocation();
              if (location != null) {
                var vec = location.first().asVec();
                var permissionFunction = this.bridgeManagement.permissionFunction();
                for (var entity : location.second().getNearbyEntities(location.first(), distance)) {
                  if (entity instanceof Player player) {
                    if (conf.bypassPermission() == null || !permissionFunction.apply(player, conf.bypassPermission())) {
                      entity.setVelocity(entity.getPosition().asVec()
                        .sub(vec)
                        .normalize()
                        .mul(conf.strength())
                        .withY(0.2));
                    }
                  }
                }
              }
            }
          }
        }
      }
    }, TaskSchedule.immediate(), TaskSchedule.tick(5));
  }

  @Override
  public @Nullable WorldPosition convertPosition(@NonNull Tuple2<Point, Instance> location) {
    return this.convertPosition(location.first(), location.second());
  }

  public @Nullable WorldPosition convertPosition(@NonNull Point pos, @NonNull Instance instance) {
    var entry = this.applicableSignConfigurationEntry();
    if (entry == null) {
      return null;
    }
    // extract the name of the instance
    var identifier = instance.getUniqueId().toString();
    return new WorldPosition(pos.x(), pos.y(), pos.z(), 0, 0, identifier, entry.targetGroup());
  }

  @Override
  protected @NonNull PlatformSign<Player, String> createPlatformSign(@NonNull Sign base) {
    return new MinestomPlatformSign(base, this.serviceRegistry, this.eventHandler, this.instanceManager);
  }
}
