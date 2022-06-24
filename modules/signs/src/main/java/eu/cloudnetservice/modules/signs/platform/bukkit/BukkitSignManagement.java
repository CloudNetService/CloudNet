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

package eu.cloudnetservice.modules.signs.platform.bukkit;

import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class BukkitSignManagement extends PlatformSignManagement<Player, Location, String> {

  protected final Plugin plugin;

  protected BukkitSignManagement(@NonNull Plugin plugin) {
    super(runnable -> {
      // check if we're already on main
      if (Bukkit.isPrimaryThread()) {
        runnable.run();
      } else {
        Bukkit.getScheduler().runTask(plugin, runnable);
      }
    });
    this.plugin = plugin;
  }

  @Override
  protected int tps() {
    return 20;
  }

  @Override
  protected void startKnockbackTask() {
    Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
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
    return new BukkitPlatformSign(base);
  }
}
