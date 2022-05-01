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

package eu.cloudnetservice.modules.signs.platform.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.plugin.Plugin;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NukkitSignManagement extends PlatformSignManagement<Player, Location> {

  protected final Plugin plugin;

  protected NukkitSignManagement(@NonNull Plugin plugin) {
    super(runnable -> {
      if (Server.getInstance().isPrimaryThread()) {
        runnable.run();
      } else {
        Server.getInstance().getScheduler().scheduleTask(plugin, runnable);
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
    Server.getInstance().getScheduler().scheduleDelayedRepeatingTask(this.plugin, () -> {
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
  public @Nullable WorldPosition convertPosition(@NotNull Location location) {
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
  protected @NonNull PlatformSign<Player> createPlatformSign(@NonNull Sign base) {
    return new NukkitPlatformSign(base);
  }
}
