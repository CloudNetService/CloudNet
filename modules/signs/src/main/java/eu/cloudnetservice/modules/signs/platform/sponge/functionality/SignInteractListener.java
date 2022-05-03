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

package eu.cloudnetservice.modules.signs.platform.sponge.functionality;

import eu.cloudnetservice.modules.signs.platform.PlatformSignManagement;
import lombok.NonNull;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.plugin.PluginContainer;

public class SignInteractListener {

  protected final PluginContainer plugin;
  protected final PlatformSignManagement<ServerPlayer, ServerLocation> signManagement;

  public SignInteractListener(
    @NonNull PluginContainer plugin,
    @NonNull PlatformSignManagement<ServerPlayer, ServerLocation> signManagement
  ) {
    this.plugin = plugin;
    this.signManagement = signManagement;
  }

  @Listener
  public void handle(@NonNull InteractBlockEvent.Secondary event, @First ServerPlayer player) {
    event.block().location().ifPresent(location -> {
      // check if a sign is at the given position
      var blockEntity = location.blockEntity().orElse(null);
      if (blockEntity instanceof Sign) {
        // get the registered sign at the given position
        var position = this.signManagement.convertPosition(location);
        var sign = this.signManagement.platformSignAt(position);

        // execute the interact action if the sign is present
        if (sign != null) {
          event.setCancelled(true);
          sign.handleInteract(player.uniqueId(), player);
        }
      }
    });
  }
}
