/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.scoreboard.TeamManager;

@Singleton
@PlatformPlugin(
  platform = "minestom",
  name = "CloudNet-SimpleNameTags",
  version = "{project.build.version}",
  authors = "CloudNetService",
  dependencies = @Dependency(name = "CloudNet-CloudPerms")
)
public class MinestomSimpleNameTagsExtension implements PlatformEntrypoint {

  private final GlobalEventHandler eventHandler;
  private final SimpleNameTagsManager<Player> nameTagsManager;

  @Inject
  public MinestomSimpleNameTagsExtension(
    @NonNull GlobalEventHandler eventHandler,
    @NonNull TeamManager teamManager,
    @NonNull ConnectionManager connectionManager,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.eventHandler = eventHandler;
    this.nameTagsManager = new MinestomSimpleNameTagsManager(
      teamManager,
      connectionManager,
      eventManager,
      permissionManagement);
  }

  @Override
  public void onLoad() {
    var node = EventNode.type("cloudnet-simplenametags", EventFilter.PLAYER);
    this.eventHandler.addChild(node.addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn));
  }

  private void handlePlayerSpawn(@NonNull PlayerSpawnEvent event) {
    if (event.isFirstSpawn()) {
      this.nameTagsManager.updateNameTagsFor(event.getPlayer());
    }
  }
}
