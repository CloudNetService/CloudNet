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

package eu.cloudnetservice.modules.signs.impl.platform.nukkit.functionality;

import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import eu.cloudnetservice.modules.signs.impl.platform.nukkit.NukkitSignManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class SignInteractListener implements Listener {

  protected final NukkitSignManagement signManagement;

  @Inject
  public SignInteractListener(@NonNull NukkitSignManagement signManagement) {
    this.signManagement = signManagement;
  }

  @EventHandler
  public void handle(@NonNull PlayerInteractEvent event) {
    if (event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && event.getBlock() != null) {
      var blockEntity = event.getBlock().getLevel().getBlockEntity(event.getBlock().getLocation());
      if (blockEntity instanceof BlockEntitySign) {
        // get the sign at the given position
        var pos = this.signManagement.convertPosition(event.getBlock().getLocation());
        var sign = this.signManagement.platformSignAt(pos);

        // execute the interact action if the sign is registered
        if (sign != null) {
          event.setCancelled(true);
          sign.handleInteract(event.getPlayer().getUniqueId(), event.getPlayer());
        }
      }
    }
  }
}
