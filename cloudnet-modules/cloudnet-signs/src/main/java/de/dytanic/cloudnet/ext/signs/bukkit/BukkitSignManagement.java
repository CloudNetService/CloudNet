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

package de.dytanic.cloudnet.ext.signs.bukkit;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitSignManagement extends AbstractSignManagement {

  private final BukkitCloudNetSignsPlugin plugin;

  protected BukkitSignManagement(BukkitCloudNetSignsPlugin plugin) {
    super();

    this.plugin = plugin;

    super.executeSearchingTask();
    super.executeStartingTask();
    super.updateSigns();
  }

  /**
   * @deprecated SignManagement should be accessed via the {@link de.dytanic.cloudnet.common.registry.IServicesRegistry}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public static BukkitSignManagement getInstance() {
    return (BukkitSignManagement) CloudNetDriver.getInstance().getServicesRegistry()
      .getFirstService(AbstractSignManagement.class);
  }

  @Override
  protected void updateSignNext(@NotNull Sign sign, @NotNull SignLayout signLayout,
    @Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
    Bukkit.getScheduler().runTask(this.plugin, () -> {
      Location location = this.toLocation(sign.getWorldPosition());
      if (location != null && location.getWorld() != null) {
        int chunkX = (int) Math.floor(location.getX()) >> 4;
        int chunkZ = (int) Math.floor(location.getZ()) >> 4;

        if (location.getWorld().isChunkLoaded(chunkX, chunkZ)) {
          Block block = location.getBlock();
          if (block.getState() instanceof org.bukkit.block.Sign) {
            org.bukkit.block.Sign bukkitSign = (org.bukkit.block.Sign) block.getState();
            this.updateSign(location, sign, bukkitSign, signLayout, serviceInfoSnapshot);
          }
        }
      }
    });
  }

  @Override
  public void cleanup() {
    Iterator<Sign> signIterator = super.signs.iterator();

    while (signIterator.hasNext()) {
      Sign sign = signIterator.next();

      Location location = this.toLocation(sign.getWorldPosition());

      if (location == null || !(location.getBlock().getState() instanceof org.bukkit.block.Sign)) {
        signIterator.remove();
        super.sendSignRemoveUpdate(sign);
      }
    }
  }

  @Override
  protected void runTaskLater(@NotNull Runnable runnable, long delay) {
    Bukkit.getScheduler().runTaskLater(this.plugin, runnable, delay);
  }

  private void updateSign(Location location, Sign sign, org.bukkit.block.Sign bukkitSign, SignLayout signLayout,
    ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(location);
    Preconditions.checkNotNull(bukkitSign);
    Preconditions.checkNotNull(signLayout);

    if (signLayout.getLines() != null && signLayout.getLines().length == 4) {
      for (int i = 0; i < 4; i++) {
        String line = ChatColor.translateAlternateColorCodes('&',
          super.replaceServiceInfo(signLayout.getLines()[i], sign.getTargetGroup(), serviceInfoSnapshot));
        bukkitSign.setLine(i, line);
      }

      bukkitSign.update();
      this.changeBlock(location, signLayout.getBlockType(), signLayout.getSubId());
    }
  }

  private void changeBlock(Location location, String blockType, int subId) {
    Preconditions.checkNotNull(location);

    if (blockType != null) {
      BlockState signBlockState = location.getBlock().getState();

      // trying to use the new block data api
      BlockFace signBlockFace = this.getSignFacing(signBlockState);

      // if this fails, trying to use the legacy api
      if (signBlockFace == null) {
        MaterialData signMaterialData = signBlockState.getData();

        if (signMaterialData instanceof org.bukkit.material.Sign) {
          org.bukkit.material.Sign sign = (org.bukkit.material.Sign) signMaterialData;
          signBlockFace = sign.isWallSign() ? sign.getFacing() : BlockFace.UP;
        }
      }

      if (signBlockFace != null) {
        BlockState backBlockState = location.getBlock().getRelative(signBlockFace.getOppositeFace()).getState();
        Material backBlockMaterial = Material.getMaterial(blockType.toUpperCase());

        if (backBlockMaterial != null) {
          backBlockState.setType(backBlockMaterial);
          if (subId > -1) {
            backBlockState.setData(new MaterialData(backBlockMaterial, (byte) subId));
          }
          backBlockState.update(true);
        }
      }
    }
  }

  /**
   * Returns the facing of the specified block state, if its block data is a WallSign from the 1.13+ spigot api
   *
   * @param blockState the block state the facing should be returned from
   * @return the facing of the block state
   */
  private BlockFace getSignFacing(BlockState blockState) {
    try {
      Method getBlockDataMethod = BlockState.class.getDeclaredMethod("getBlockData");
      Object blockData = getBlockDataMethod.invoke(blockState);
      Class<?> wallSignClass = Class.forName("org.bukkit.block.data.type.WallSign");

      if (wallSignClass.isInstance(blockData)) {
        Method getFacingMethod = wallSignClass.getMethod("getFacing");
        return (BlockFace) getFacingMethod.invoke(blockData);
      }

      return BlockFace.UP;
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
      return null;
    }
  }

  public Location toLocation(WorldPosition worldPosition) {
    Preconditions.checkNotNull(worldPosition);

    World world = Bukkit.getWorld(worldPosition.getWorld());
    return world == null ? null : new Location(world, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
  }

  public BukkitCloudNetSignsPlugin getPlugin() {
    return this.plugin;
  }
}
