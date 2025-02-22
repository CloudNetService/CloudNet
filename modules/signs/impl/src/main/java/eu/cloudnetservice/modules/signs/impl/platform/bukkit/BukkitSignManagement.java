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

package eu.cloudnetservice.modules.signs.impl.platform.bukkit;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.impl.InternalSignManagement;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSignManagement;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "bukkit", types = {
  PlatformSignManagement.class,
  InternalSignManagement.class,
  SignManagement.class
})
public class BukkitSignManagement extends PlatformSignManagement<Player, Location, String> {

  protected final Plugin plugin;
  protected final PluginManager pluginManager;
  protected final ServiceRegistry serviceRegistry;
  protected final BukkitScheduler scheduler;

  @Inject
  protected BukkitSignManagement(
    @NonNull Plugin plugin,
    @NonNull Server server,
    @NonNull BukkitScheduler scheduler,
    @NonNull EventManager eventManager,
    @NonNull PluginManager pluginManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull WrapperConfiguration wrapperConfig,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull @Named("taskScheduler") ScheduledExecutorService executorService
  ) {
    super(eventManager, runnable -> {
      // only schedule tasks if the plugin is enabled, bukkit does not allow scheduling while disabled
      if (plugin.isEnabled()) {
        // check if we're already on main
        if (server.isPrimaryThread()) {
          runnable.run();
        } else {
          scheduler.runTask(plugin, runnable);
        }
      }
    }, wrapperConfig, serviceProvider, executorService);

    this.plugin = plugin;
    this.scheduler = scheduler;
    this.pluginManager = pluginManager;
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  protected int tps() {
    return 20;
  }

  @Override
  protected void startKnockbackTask() {
    this.scheduler.runTaskTimer(this.plugin, () -> {
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var conf = entry.knockbackConfiguration();
        if (conf.validAndEnabled()) {
          var distance = conf.distance();
          // find all signs which need to knock back the player
          for (var sign : this.platformSigns.values()) {
            if (sign.needsUpdates() && sign.exists() && sign instanceof BukkitPlatformSign bukkitSign) {
              var location = bukkitSign.signLocation();
              if (location != null) {
                var vec = location.toVector();
                for (var entity : location.getWorld().getNearbyEntities(location, distance, distance, distance)) {
                  if (entity instanceof Player player
                    && (conf.bypassPermission() == null || !player.hasPermission(conf.bypassPermission()))) {
                    entity.setVelocity(entity.getLocation().toVector()
                      .subtract(vec)
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
      location.getWorld().getName(),
      entry.targetGroup());
  }

  @Override
  protected @NonNull PlatformSign<Player, String> createPlatformSign(@NonNull Sign base) {
    return new BukkitPlatformSign(base, this.plugin.getServer(), this.pluginManager, this.serviceRegistry);
  }
}
