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

package eu.cloudnetservice.plugins.simplenametags.minestom;

import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extensions.Extension;

public class MinestomSimpleNameTagsExtension extends Extension {

  private final SimpleNameTagsManager<Player> nameTagsManager = new MinestomSimpleNameTagsManager();

  @Override
  public void initialize() {
    var node = EventNode.type("cloudnet-simplenametags", EventFilter.PLAYER);
    MinecraftServer.getGlobalEventHandler().addChild(node.addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn));
  }

  @Override
  public void terminate() {
  }

  private void handlePlayerSpawn(@NonNull PlayerSpawnEvent event) {
    if (event.isFirstSpawn()) {
      this.nameTagsManager.updateNameTagsFor(event.getPlayer());
    }
  }
}
