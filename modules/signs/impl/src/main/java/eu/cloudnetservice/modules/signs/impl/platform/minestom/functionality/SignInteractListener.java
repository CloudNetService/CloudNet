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

package eu.cloudnetservice.modules.signs.impl.platform.minestom.functionality;

import eu.cloudnetservice.modules.signs.impl.platform.minestom.MinestomSignManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerBlockInteractEvent;

@Singleton
public class SignInteractListener {

  private final MinestomSignManagement signManagement;

  @Inject
  public SignInteractListener(
    @NonNull GlobalEventHandler eventHandler,
    @NonNull MinestomSignManagement signManagement
  ) {
    this.signManagement = signManagement;

    eventHandler.addListener(PlayerBlockInteractEvent.class, this::handleSignInteract);
  }

  private void handleSignInteract(@NonNull PlayerBlockInteractEvent event) {
    var block = event.getBlock();
    var instance = event.getPlayer().getInstance();
    if (block.name().contains("sign") && instance != null) {
      var pos = this.signManagement.convertPosition(event.getBlockPosition(), instance);
      var sign = this.signManagement.platformSignAt(pos);

      if (sign != null) {
        event.setCancelled(true);
        sign.handleInteract(event.getPlayer().getUuid(), event.getPlayer());
      }
    }
  }
}
