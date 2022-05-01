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
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.TextFormat;
import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.platform.nukkit.event.NukkitCloudSignInteractEvent;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NukkitPlatformSign extends PlatformSign<Player> {

  private static final Joiner TEXT_JOINER = Joiner.on('\n').useForNull("");

  private Location signLocation;

  public NukkitPlatformSign(@NonNull Sign base) {
    super(base);
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

      // set the sign lines
      var lines = new String[4];
      for (int i = 0; i < Math.min(4, layout.lines().size()); i++) {
        lines[i] = TextFormat.colorize(layout.lines().get(i));
      }

      // this is a bit hacky as the nukkit api is just not allowing us to make is easier
      sign.namedTag.putString("Text", TEXT_JOINER.join(lines));
      sign.setDirty();
      sign.spawnToAll();

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
    Server.getInstance().getPluginManager().callEvent(event);

    return event.isCancelled() ? null : event.target();
  }

  public @Nullable Location signLocation() {
    // check if the location is already available
    if (this.signLocation != null) {
      return this.signLocation;
    }

    // check if the world associated with the sign is available
    var pos = this.base.location();
    var level = Server.getInstance().getLevelByName(pos.world());
    if (level == null) {
      return null;
    }

    return this.signLocation = new Location(pos.x(), pos.y(), pos.z(), level);
  }
}
