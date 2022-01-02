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

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.platform.AbstractPlatformSignManagement;
import java.util.Set;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.Nullable;

public class BukkitSignManagement extends AbstractPlatformSignManagement<org.bukkit.block.Sign> {

  protected final Plugin plugin;

  protected BukkitSignManagement(Plugin plugin) {
    this.plugin = plugin;
  }

  public static BukkitSignManagement defaultInstance() {
    return (BukkitSignManagement) CloudNetDriver.instance().servicesRegistry()
      .firstService(SignManagement.class);
  }

  @Override
  protected void pushUpdates(@NonNull Set<Sign> signs, @NonNull SignLayout layout) {
    if (Bukkit.isPrimaryThread()) {
      this.pushUpdates0(signs, layout);
    } else if (this.plugin.isEnabled()) {
      Bukkit.getScheduler().runTask(this.plugin, () -> this.pushUpdates0(signs, layout));
    }
  }

  protected void pushUpdates0(@NonNull Set<Sign> signs, @NonNull SignLayout layout) {
    for (var sign : signs) {
      this.pushUpdate(sign, layout);
    }
  }

  @Override
  protected void pushUpdate(@NonNull Sign sign, @NonNull SignLayout layout) {
    if (Bukkit.isPrimaryThread()) {
      this.pushUpdate0(sign, layout);
    } else if (this.plugin.isEnabled()) {
      Bukkit.getScheduler().runTask(this.plugin, () -> this.pushUpdate0(sign, layout));
    }
  }

  protected void pushUpdate0(@NonNull Sign sign, @NonNull SignLayout layout) {
    var location = this.worldPositionToLocation(sign.location());
    if (location != null) {
      var chunkX = NumberConversions.floor(location.getX()) >> 4;
      var chunkZ = NumberConversions.floor(location.getZ()) >> 4;

      if (location.getWorld().isChunkLoaded(chunkX, chunkZ)) {
        var block = location.getBlock();
        if (block.getState() instanceof org.bukkit.block.Sign bukkitSign) {
          BukkitCompatibility.signGlowing(bukkitSign, layout);

          var replaced = this.replaceLines(sign, layout);
          if (replaced != null) {
            for (var i = 0; i < 4; i++) {
              bukkitSign.setLine(i, ChatColor.translateAlternateColorCodes('&', replaced[i]));
            }

            bukkitSign.update();
          }

          this.changeBlock(block, layout);
        }
      }
    }
  }

  @SuppressWarnings("deprecation") // legacy 1.8 support...
  protected void changeBlock(@NonNull Block block, @NonNull SignLayout layout) {
    var material = Material.getMaterial(layout.blockMaterial().toUpperCase());
    if (material != null && material.isBlock()) {
      var face = BukkitCompatibility.facing(block.getState());
      if (face != null) {
        var behind = block.getRelative(face.getOppositeFace()).getState();
        if (layout.blockSubId() >= 0) {
          behind.setData(new MaterialData(material, (byte) layout.blockSubId()));
        } else {
          behind.setType(material);
        }

        behind.update(true);
      }
    }
  }

  @Override
  public @Nullable Sign signAt(@NonNull org.bukkit.block.Sign sign, @NonNull String group) {
    return this.signAt(this.locationToWorldPosition(sign.getLocation(), group));
  }

  @Override
  public @Nullable Sign createSign(@NonNull org.bukkit.block.Sign sign, @NonNull String group) {
    return this.createSign(sign, group, null);
  }

  @Override
  public @Nullable Sign createSign(
    @NonNull org.bukkit.block.Sign sign,
    @NonNull String group,
    @Nullable String templatePath
  ) {
    var entry = this.applicableSignConfigurationEntry();
    if (entry != null) {
      var created = new Sign(
        group,
        templatePath,
        this.locationToWorldPosition(sign.getLocation(), entry.targetGroup()));
      this.createSign(created);
      return created;
    }
    return null;
  }

  @Override
  public void deleteSign(@NonNull org.bukkit.block.Sign sign, @NonNull String group) {
    this.deleteSign(this.locationToWorldPosition(sign.getLocation(), group));
  }

  @Override
  public int removeMissingSigns() {
    var removed = 0;
    for (var position : this.signs.keySet()) {
      var location = this.worldPositionToLocation(position);
      if (location == null || !(location.getBlock().getState() instanceof org.bukkit.block.Sign)) {
        this.deleteSign(position);
        removed++;
      }
    }

    return removed;
  }

  @Override
  protected void startKnockbackTask() {
    Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var configuration = entry.knockbackConfiguration();
        if (configuration.validAndEnabled()) {
          var distance = configuration.distance();

          for (var value : this.signs.values()) {
            var location = this.worldPositionToLocation(value.location());
            if (location != null) {
              var chunkX = (int) Math.floor(location.getX()) >> 4;
              var chunkZ = (int) Math.floor(location.getZ()) >> 4;

              if (location.getWorld().isChunkLoaded(chunkX, chunkZ)
                && location.getBlock().getState() instanceof org.bukkit.block.Sign) {
                location.getWorld()
                  .getNearbyEntities(location, distance, distance, distance)
                  .stream()
                  .filter(entity -> entity instanceof Player && (configuration.bypassPermission() == null
                    || !entity.hasPermission(configuration.bypassPermission())))
                  .forEach(entity -> entity.setVelocity(entity.getLocation().toVector()
                    .subtract(location.toVector())
                    .normalize()
                    .multiply(configuration.strength())));
              }
            }
          }
        }
      }
    }, 0, 5);
  }

  protected @NonNull WorldPosition locationToWorldPosition(@NonNull Location location, @NonNull String group) {
    return new WorldPosition(
      location.getX(),
      location.getY(),
      location.getZ(),
      0,
      0,
      location.getWorld().getName(),
      group);
  }

  protected @Nullable Location worldPositionToLocation(@NonNull WorldPosition position) {
    var world = Bukkit.getWorld(position.world());
    return world == null ? null : new Location(world, position.x(), position.y(), position.z());
  }
}
