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
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.level.Location;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.TextFormat;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.platform.AbstractPlatformSignManagement;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NukkitSignManagement extends AbstractPlatformSignManagement<BlockEntitySign> {

  protected final Plugin plugin;

  public NukkitSignManagement(Plugin plugin) {
    this.plugin = plugin;
  }

  public static NukkitSignManagement defaultInstance() {
    return (NukkitSignManagement) CloudNetDriver.instance().servicesRegistry()
      .firstService(SignManagement.class);
  }

  @Override
  protected void pushUpdates(@NonNull Set<Sign> signs, @NonNull SignLayout layout) {
    if (Server.getInstance().isPrimaryThread()) {
      this.pushUpdates0(signs, layout);
    } else if (this.plugin.isEnabled()) {
      Server.getInstance().getScheduler().scheduleTask(this.plugin, () -> this.pushUpdates0(signs, layout));
    }
  }

  protected void pushUpdates0(@NonNull Set<Sign> signs, @NonNull SignLayout layout) {
    for (var sign : signs) {
      this.pushUpdate(sign, layout);
    }
  }

  @Override
  protected void pushUpdate(@NonNull Sign sign, @NonNull SignLayout layout) {
    if (Server.getInstance().isPrimaryThread()) {
      this.pushUpdate0(sign, layout);
    } else if (this.plugin.isEnabled()) {
      Server.getInstance().getScheduler().scheduleTask(this.plugin, () -> this.pushUpdate0(sign, layout));
    }
  }

  protected void pushUpdate0(@NonNull Sign sign, @NonNull SignLayout layout) {
    var location = this.locationFromWorldPosition(sign.location());
    if (location != null && location.getLevel().isChunkLoaded(location.getChunkX(), location.getChunkZ())) {
      var blockEntity = location.getLevel().getBlockEntity(location);
      if (blockEntity instanceof BlockEntitySign entitySign) {

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

  protected void changeBlock(@NonNull Block block, @NonNull SignLayout layout) {
    var itemId = Ints.tryParse(layout.blockMaterial());
    if (itemId != null && block instanceof Faceable) {
      var face =
        block instanceof BlockWallSign ? ((Faceable) block).getBlockFace().getOpposite() : BlockFace.DOWN;

      var backLocation = block.getSide(face).getLocation();
      backLocation.getLevel()
        .setBlock(backLocation, Block.get((int) itemId, Math.max(0, layout.blockSubId())), true, true);
    }
  }

  @Override
  public @Nullable Sign signAt(@NonNull BlockEntitySign blockEntitySign, @NonNull String group) {
    return this.signAt(this.locationToWorldPosition(blockEntitySign.getLocation(), group));
  }

  @Override
  public @Nullable Sign createSign(@NonNull BlockEntitySign blockEntitySign, @NonNull String group) {
    return this.createSign(blockEntitySign, group, null);
  }

  @Override
  public @Nullable Sign createSign(
    @NonNull BlockEntitySign blockEntitySign,
    @NonNull String group,
    @Nullable String templatePath
  ) {
    var entry = this.applicableSignConfigurationEntry();
    if (entry != null) {
      var sign = new Sign(
        group,
        templatePath,
        this.locationToWorldPosition(blockEntitySign.getLocation(), entry.targetGroup()));
      this.createSign(sign);
      return sign;
    }
    return null;
  }

  @Override
  public void deleteSign(@NonNull BlockEntitySign blockEntitySign, @NonNull String group) {
    this.deleteSign(this.locationToWorldPosition(blockEntitySign.getLocation(), group));
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
      var entry = this.applicableSignConfigurationEntry();
      if (entry != null) {
        var configuration = entry.knockbackConfiguration();
        if (configuration.validAndEnabled()) {
          var distance = configuration.distance();

          for (var value : this.signs.values()) {
            var location = this.locationFromWorldPosition(value.location());
            if (location != null && location.getLevel().isChunkLoaded(location.getChunkX(), location.getChunkZ())
              && location.getLevel().getBlockEntity(location) instanceof BlockEntitySign) {
              var axisAlignedBB = new SimpleAxisAlignedBB(location, location)
                .expand(distance, distance, distance);

              for (var entity : location.getLevel().getNearbyEntities(axisAlignedBB)) {
                if (entity instanceof Player && (configuration.bypassPermission() == null
                  || !((Player) entity).hasPermission(configuration.bypassPermission()))) {
                  var vector = entity.getPosition()
                    .subtract(location)
                    .normalize()
                    .multiply(configuration.strength());
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

  protected @NonNull WorldPosition locationToWorldPosition(@NonNull Location location, @NonNull String group) {
    return new WorldPosition(
      location.getX(),
      location.getY(),
      location.getZ(),
      0,
      0,
      location.getLevel().getName(),
      group);
  }

  protected @Nullable Location locationFromWorldPosition(@NonNull WorldPosition position) {
    var level = Server.getInstance().getLevelByName(position.world());
    return level == null ? null : new Location(position.x(), position.y(), position.z(), level);
  }
}
