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

package eu.cloudnetservice.cloudnet.ext.signs.platform.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.TextFormat;
import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignLayout;
import eu.cloudnetservice.cloudnet.ext.signs.platform.AbstractPlatformSignManagement;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NukkitSignManagement extends AbstractPlatformSignManagement<BlockEntitySign> {

  protected final Plugin plugin;

  public NukkitSignManagement(Plugin plugin) {
    this.plugin = plugin;
  }

  public static NukkitSignManagement getDefaultInstance() {
    return (NukkitSignManagement) CloudNetDriver.getInstance().getServicesRegistry()
      .getFirstService(SignManagement.class);
  }

  @Override
  protected void pushUpdates(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
    if (Server.getInstance().isPrimaryThread()) {
      this.pushUpdates0(signs, layout);
    } else if (this.plugin.isEnabled()) {
      Server.getInstance().getScheduler().scheduleTask(this.plugin, () -> this.pushUpdates0(signs, layout));
    }
  }

  protected void pushUpdates0(@NotNull Set<Sign> signs, @NotNull SignLayout layout) {
    for (var sign : signs) {
      this.pushUpdate(sign, layout);
    }
  }

  @Override
  protected void pushUpdate(@NotNull Sign sign, @NotNull SignLayout layout) {
    if (Server.getInstance().isPrimaryThread()) {
      this.pushUpdate0(sign, layout);
    } else if (this.plugin.isEnabled()) {
      Server.getInstance().getScheduler().scheduleTask(this.plugin, () -> this.pushUpdate0(sign, layout));
    }
  }

  protected void pushUpdate0(@NotNull Sign sign, @NotNull SignLayout layout) {
    var location = this.locationFromWorldPosition(sign.getLocation());
    if (location != null && location.getLevel().isChunkLoaded(location.getChunkX(), location.getChunkZ())) {
      var blockEntity = location.getLevel().getBlockEntity(location);
      if (blockEntity instanceof BlockEntitySign) {
        var entitySign = (BlockEntitySign) blockEntity;

        var lines = this.replaceLines(sign, layout);
        if (lines != null) {
          for (var i = 0; i < 4; i++) {
            lines[i] = TextFormat.colorize('&', lines[i]);
          }

          entitySign.setText(lines);
          this.changeBlock(entitySign.getBlock(), layout);
        }
      }
    }
  }

  protected void changeBlock(@NotNull Block block, @NotNull SignLayout layout) {
    var itemId = layout.getBlockMaterial() == null ? null : Ints.tryParse(layout.getBlockMaterial());
    if (itemId != null && block instanceof Faceable) {
      var face =
        block instanceof BlockWallSign ? ((Faceable) block).getBlockFace().getOpposite() : BlockFace.DOWN;

      var backLocation = block.getSide(face).getLocation();
      backLocation.getLevel()
        .setBlock(backLocation, Block.get((int) itemId, Math.max(0, layout.getBlockSubId())), true, true);
    }
  }

  @Override
  public @Nullable Sign getSignAt(@NotNull BlockEntitySign blockEntitySign) {
    return this.getSignAt(this.locationToWorldPosition(blockEntitySign.getLocation()));
  }

  @Override
  public @Nullable Sign createSign(@NotNull BlockEntitySign blockEntitySign, @NotNull String group) {
    return this.createSign(blockEntitySign, group, null);
  }

  @Override
  public @Nullable Sign createSign(
    @NotNull BlockEntitySign blockEntitySign,
    @NotNull String group,
    @Nullable String templatePath
  ) {
    var entry = this.getApplicableSignConfigurationEntry();
    if (entry != null) {
      var sign = new Sign(
        group,
        templatePath,
        this.locationToWorldPosition(blockEntitySign.getLocation(), entry.getTargetGroup()));
      this.createSign(sign);
      return sign;
    }
    return null;
  }

  @Override
  public void deleteSign(@NotNull BlockEntitySign blockEntitySign) {
    this.deleteSign(this.locationToWorldPosition(blockEntitySign.getLocation()));
  }

  @Override
  public int removeMissingSigns() {
    var removed = 0;
    for (var position : this.signs.keySet()) {
      var location = this.locationFromWorldPosition(position);
      if (location == null || !(location.getLevel().getBlockEntity(location) instanceof BlockEntitySign)) {
        this.deleteSign(position);
        removed++;
      }
    }
    return removed;
  }

  @Override
  protected void startKnockbackTask() {
    Server.getInstance().getScheduler().scheduleDelayedRepeatingTask(this.plugin, () -> {
      var entry = this.getApplicableSignConfigurationEntry();
      if (entry != null) {
        var configuration = entry.getKnockbackConfiguration();
        if (configuration.isValidAndEnabled()) {
          var distance = configuration.getDistance();

          for (var value : this.signs.values()) {
            var location = this.locationFromWorldPosition(value.getLocation());
            if (location != null && location.getLevel().isChunkLoaded(location.getChunkX(), location.getChunkZ())
              && location.getLevel().getBlockEntity(location) instanceof BlockEntitySign) {
              var axisAlignedBB = new SimpleAxisAlignedBB(location, location)
                .expand(distance, distance, distance);

              for (var entity : location.getLevel().getNearbyEntities(axisAlignedBB)) {
                if (entity instanceof Player && (configuration.getBypassPermission() == null
                  || !((Player) entity).hasPermission(configuration.getBypassPermission()))) {
                  var vector = entity.getPosition()
                    .subtract(location)
                    .normalize()
                    .multiply(configuration.getStrength());
                  vector.y = 0.2D;
                  entity.setMotion(vector);
                }
              }
            }
          }
        }
      }
    }, 0, 5);
  }

  protected @NotNull WorldPosition locationToWorldPosition(@NotNull Location location) {
    return new WorldPosition(
      location.getX(),
      location.getY(),
      location.getZ(),
      0,
      0,
      location.getLevel().getName(),
      null);
  }

  protected @NotNull WorldPosition locationToWorldPosition(@NotNull Location location, @NotNull String group) {
    return new WorldPosition(
      location.getX(),
      location.getY(),
      location.getZ(),
      0,
      0,
      location.getLevel().getName(),
      group);
  }

  protected @Nullable Location locationFromWorldPosition(@NotNull WorldPosition position) {
    var level = Server.getInstance().getLevelByName(position.getWorld());
    return level == null ? null : new Location(position.getX(), position.getY(), position.getZ(), level);
  }
}
