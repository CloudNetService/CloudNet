/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.platform.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.scheduler.ServerScheduler;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.injection.Service;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
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
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "nukkit", types = {PlatformSignManagement.class, SignManagement.class})
public class NukkitSignManagement extends PlatformSignManagement<Player, Location, String> {

  private final Server server;
  private final Plugin plugin;
  private final ServerScheduler scheduler;
  private final PluginManager pluginManager;
  private final PlayerManager playerManager;

  @Inject
  protected NukkitSignManagement(
    @NonNull Server server,
    @NonNull Plugin plugin,
    @NonNull ServerScheduler scheduler,
    @NonNull EventManager eventManager,
    @NonNull PluginManager pluginManager,
    @NonNull @Service PlayerManager playerManager,
    @NonNull WrapperConfiguration wrapperConfig,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull @Named("taskScheduler") ScheduledExecutorService executorService
  ) {
    super(
      eventManager,
      runnable -> {
        if (server.isPrimaryThread()) {
          runnable.run();
        } else {
          scheduler.scheduleTask(plugin, runnable);
        }
      },
      wrapperConfig,
      serviceProvider,
      executorService);

    this.plugin = plugin;
    this.server = server;
    this.scheduler = scheduler;
    this.pluginManager = pluginManager;
    this.playerManager = playerManager;
  }

  @Override
  protected int tps() {
    return 20;
  }

  @Override
  protected void startKnockbackTask() {
    this.scheduler.scheduleDelayedRepeatingTask(this.plugin, () -> {
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var conf = entry.knockbackConfiguration();
        if (conf.validAndEnabled()) {
          var distance = conf.distance();
          // find all signs which need to knock back the player
          for (var sign : this.platformSigns.values()) {
            if (sign.needsUpdates() && sign.exists() && sign instanceof NukkitPlatformSign nukkitSign) {
              var location = nukkitSign.signLocation();
              if (location != null) {
                var bb = new SimpleAxisAlignedBB(location, location).expand(distance, distance, distance);
                for (var entity : location.getLevel().getNearbyEntities(bb)) {
                  if (entity instanceof Player player
                    && (conf.bypassPermission() == null || !player.hasPermission(conf.bypassPermission()))) {
                    entity.setMotion(entity.getPosition()
                      .subtract(location)
                      .normalize()
                      .multiply(conf.strength())
                      .setY(0.2));
                  }
                }
              }
            }
          }
        }
      }
    }, 0, 5);
  }

  @Override
  public @Nullable WorldPosition convertPosition(@NonNull Location location) {
    var entry = this.applicableSignConfigurationEntry();
    if (entry == null) {
      return null;
    }

    return new WorldPosition(
      location.getX(),
      location.getY(),
      location.getZ(),
      0,
      0,
      location.getLevel().getName(),
      entry.targetGroup());
  }

  @Override
  protected @NonNull PlatformSign<Player, String> createPlatformSign(@NonNull Sign base) {
    return new NukkitPlatformSign(base, this.server, this.pluginManager, this.playerManager);
  }
}
