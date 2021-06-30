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

package de.dytanic.cloudnet.ext.signs.nukkit;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.Faceable;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignLayout;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NukkitSignManagement extends AbstractSignManagement {

  private final NukkitCloudNetSignsPlugin plugin;

  NukkitSignManagement(NukkitCloudNetSignsPlugin plugin) {
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
  public static NukkitSignManagement getInstance() {
    return (NukkitSignManagement) CloudNetDriver.getInstance().getServicesRegistry()
      .getFirstService(AbstractSignManagement.class);
  }

  private void runSync(Runnable runnable) {
    if (Server.getInstance().isPrimaryThread()) {
      runnable.run();
    } else {
      Server.getInstance().getScheduler().scheduleTask(this.plugin, runnable);
    }
  }

  @Override
  protected void updateSignNext(@NotNull Sign sign, @NotNull SignLayout signLayout,
    @Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
    this.runSync(() -> {
      Location location = this.toLocation(sign.getWorldPosition());
      if (location != null && location.getLevel().isChunkLoaded(location.getChunkX(), location.getChunkZ())) {
        BlockEntity blockEntity = location.getLevel().getBlockEntity(location);
        if (blockEntity instanceof BlockEntitySign) {
          this.updateSign(sign, (BlockEntitySign) blockEntity, signLayout, serviceInfoSnapshot);
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

      if (location == null || !(location.getLevel().getBlockEntity(location) instanceof BlockEntitySign)) {
        signIterator.remove();
        super.sendSignRemoveUpdate(sign);
      }
    }
  }

  @Override
  protected void runTaskLater(@NotNull Runnable runnable, long delay) {
    Server.getInstance().getScheduler().scheduleDelayedTask(this.plugin, runnable, Math.toIntExact(delay));
  }

  private void updateSign(Sign sign, BlockEntitySign nukkitSign, SignLayout signLayout,
    ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(nukkitSign);
    Preconditions.checkNotNull(signLayout);

    if (signLayout.getLines() != null &&
      signLayout.getLines().length == 4) {

      String[] lines = new String[4];

      for (int i = 0; i < lines.length; i++) {
        String line = super.replaceServiceInfo(signLayout.getLines()[i], sign.getTargetGroup(), serviceInfoSnapshot)
          .replace('&', '§');
        lines[i] = line;
      }

      nukkitSign.setText(lines);

      this.changeBlock(nukkitSign, signLayout.getBlockType(), signLayout.getSubId());
    }
  }

  private void changeBlock(BlockEntitySign nukkitSign, String blockType, int subId) {
    Preconditions.checkNotNull(nukkitSign);

    if (blockType != null) {

      Block block = nukkitSign.getBlock();

      if (block instanceof Faceable) {
        Faceable faceable = (Faceable) block;
        BlockFace blockFace = block instanceof BlockWallSign ? faceable.getBlockFace().getOpposite() : BlockFace.DOWN;

        Location backBlockLocation = block.getSide(blockFace).getLocation();

        int itemId;
        try {
          itemId = Integer.parseInt(blockType);
        } catch (NumberFormatException exception) {
          return;
        }

        backBlockLocation.getLevel().setBlock(backBlockLocation, Block.get(itemId, Math.max(0, subId)), true, true);
      }


    }
  }

  public Location toLocation(WorldPosition position) {
    Preconditions.checkNotNull(position);

    Level level = Server.getInstance().getLevelByName(position.getWorld());
    return level == null ? null : new Location(position.getX(), position.getY(), position.getZ(), level);
  }

  public NukkitCloudNetSignsPlugin getPlugin() {
    return this.plugin;
  }
}
