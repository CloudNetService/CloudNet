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

package eu.cloudnetservice.modules.signs.impl.platform.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.level.Location;
import cn.nukkit.math.BlockFace;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.TextFormat;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.impl.platform.nukkit.event.NukkitCloudSignInteractEvent;
import java.util.Arrays;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NukkitPlatformSign extends PlatformSign<Player, String> {

  private final String[] lineBuffer = new String[4];

  private final Server server;
  private final PluginManager pluginManager;

  private Location signLocation;

  public NukkitPlatformSign(
    @NonNull Sign base,
    @NonNull Server server,
    @NonNull PluginManager pluginManager,
    @NonNull ServiceRegistry serviceRegistry
  ) {
    super(base, serviceRegistry, input -> TextFormat.colorize('&', input));

    this.server = server;
    this.pluginManager = pluginManager;
  }

  @Override
  public boolean exists() {
    // check if the location associated with the sign is available
    var location = this.signLocation();
    if (location == null) {
      return false;
    }

    // get the block at the given location
    var blockEntity = location.getLevel().getBlockEntity(location);
    return blockEntity instanceof BlockEntitySign;
  }

  @Override
  public boolean needsUpdates() {
    // check if the location associated with the sign is available
    var location = this.signLocation();
    if (location == null) {
      return false;
    }

    // check if the associated chunk is loaded
    return location.getLevel().isChunkLoaded(location.getChunkX(), location.getChunkZ());
  }

  @Override
  public void updateSign(@NonNull SignLayout layout) {
    // check if the location associated with the sign is available
    var location = this.signLocation();
    if (location == null) {
      return;
    }

    // get the block at the given location
    var blockEntity = location.getLevel().getBlockEntity(location);
    if (blockEntity instanceof BlockEntitySign sign) {
      // set the glowing status if needed
      sign.setGlowing(layout.glowingColor() != null);

      // remove all old sign lines from the lines buffer
      // this is not thread safe at all, but updates to the sign should only be made from the server primary thread
      // anyway which makes this somewhat reliable to do
      var lines = this.lineBuffer;
      Arrays.fill(lines, "");

      // set the sign lines
      this.changeSignLines(layout, (index, line) -> lines[index] = line);
      sign.setText(lines);

      // change the block behind the sign
      var block = sign.getBlock();
      var itemId = Ints.tryParse(layout.blockMaterial());
      if (itemId != null && block instanceof Faceable) {
        var face = block instanceof BlockWallSign faced ? faced.getBlockFace().getOpposite() : BlockFace.DOWN;

        var backLocation = block.getSide(face).getLocation();
        backLocation.getLevel().setBlock(
          backLocation,
          Block.get((int) itemId, Math.max(0, layout.blockSubId())),
          true,
          true);
      }
    }
  }

  @Override
  public @Nullable ServiceInfoSnapshot callSignInteractEvent(@NonNull Player player) {
    var event = new NukkitCloudSignInteractEvent(player, this);
    this.pluginManager.callEvent(event);

    return event.isCancelled() ? null : event.target();
  }

  public @Nullable Location signLocation() {
    // check if the location is already available
    if (this.signLocation != null) {
      return this.signLocation;
    }

    // check if the world associated with the sign is available
    var pos = this.base.location();
    var level = this.server.getLevelByName(pos.world());
    if (level == null) {
      return null;
    }

    return this.signLocation = new Location(pos.x(), pos.y(), pos.z(), level);
  }
}
