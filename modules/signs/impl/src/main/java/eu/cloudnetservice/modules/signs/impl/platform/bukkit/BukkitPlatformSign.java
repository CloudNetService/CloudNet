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

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.configuration.SignLayout;
import eu.cloudnetservice.modules.signs.impl.platform.PlatformSign;
import eu.cloudnetservice.modules.signs.impl.platform.bukkit.event.BukkitCloudSignInteractEvent;
import eu.cloudnetservice.utils.base.StringUtil;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.Nullable;

final class BukkitPlatformSign extends PlatformSign<Player, String> {

  // lazy initialized once available
  private Location signLocation;
  private final Server server;
  private final PluginManager pluginManager;

  public BukkitPlatformSign(
    @NonNull Sign base,
    @NonNull Server server,
    @NonNull PluginManager pluginManager,
    @NonNull ServiceRegistry serviceRegistry
  ) {
    super(base, serviceRegistry, input -> ChatColor.translateAlternateColorCodes('&', input));
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

    // checks if the type of the block at the sign location is a sign. The material name check is not the safest way
    // of doing that, but the safe way would be to use "getBlockState" which always captures a new state of the block
    // and is extremely heavy when executed often, so this way is much more lightweight for that task
    var type = location.getBlock().getType();
    return type.name().contains("SIGN");
  }

  @Override
  public boolean needsUpdates() {
    // check if the location associated with the sign is available
    var location = this.signLocation();
    if (location == null) {
      return false;
    }

    // check if the associated chunk is loaded
    var chunkX = NumberConversions.floor(location.getX()) >> 4;
    var chunkZ = NumberConversions.floor(location.getZ()) >> 4;

    return location.getWorld().isChunkLoaded(chunkX, chunkZ);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void updateSign(@NonNull SignLayout layout) {
    // check if the location associated with the sign is available
    var location = this.signLocation();
    if (location == null) {
      return;
    }

    // get the block at the given location
    var state = location.getBlock().getState();
    if (state instanceof org.bukkit.block.Sign sign) {
      // set the glowing status if needed
      BukkitCompatibility.signGlowing(sign, layout);

      // set the sign lines
      this.changeSignLines(layout, sign::setLine);
      sign.update();

      // change the block behind the sign
      var material = Material.getMaterial(StringUtil.toUpper(layout.blockMaterial()));
      if (material != null && material.isBlock()) {
        var facing = BukkitCompatibility.facing(sign);
        if (facing != null) {
          // set the type of the block behind the sign
          var behind = state.getBlock().getRelative(facing.getOppositeFace());
          behind.setType(material);

          // set the block sub id if needed
          if (layout.blockSubId() >= 0) {
            behind.setData((byte) layout.blockSubId());
          }
        }
      }
    }
  }

  @Override
  public @Nullable ServiceInfoSnapshot callSignInteractEvent(@NonNull Player player) {
    var event = new BukkitCloudSignInteractEvent(player, this);
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
    var world = this.server.getWorld(pos.world());
    if (world == null) {
      return null;
    }

    // initialize and return the location
    return this.signLocation = new Location(world, pos.x(), pos.y(), pos.z());
  }
}
