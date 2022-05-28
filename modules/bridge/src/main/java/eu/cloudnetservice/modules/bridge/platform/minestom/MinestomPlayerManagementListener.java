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

package eu.cloudnetservice.modules.bridge.platform.minestom;

import eu.cloudnetservice.ext.adventure.AdventureSerializerUtil;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.helper.ServerPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.wrapper.Wrapper;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

public final class MinestomPlayerManagementListener {

  private final PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management;

  public MinestomPlayerManagementListener(
    @NonNull PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management
  ) {
    this.management = management;
    // listen on these events and redirect them into the methods
    var node = EventNode.type("cloudnet-bridge", EventFilter.PLAYER);
    MinecraftServer.getGlobalEventHandler().addChild(node
      .addListener(AsyncPlayerPreLoginEvent.class, this::handleLogin)
      .addListener(PlayerSpawnEvent.class, this::handleJoin)
      .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));
  }

  private void handleLogin(@NonNull AsyncPlayerPreLoginEvent event) {
    var player = event.getPlayer();
    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.maintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
        player.kick(AdventureSerializerUtil.serialize(this.management.configuration().message(
          player.getLocale(),
          "server-join-cancel-because-maintenance")));
        return;
      }
      // check if a custom permission is required to join
      var permission = task.properties().getString("requiredPermission");
      if (permission != null && !player.hasPermission(permission)) {
        player.kick(AdventureSerializerUtil.serialize(this.management.configuration().message(
          player.getLocale(),
          "server-join-cancel-because-permission")));
      }
    }
  }

  private void handleJoin(@NonNull PlayerSpawnEvent event) {
    ServerPlatformHelper.sendChannelMessageLoginSuccess(
      event.getPlayer().getUuid(),
      this.management.createPlayerInformation(event.getPlayer()));
    // update the service info
    Wrapper.instance().publishServiceInfoUpdate();
  }

  private void handleDisconnect(@NonNull PlayerDisconnectEvent event) {
    ServerPlatformHelper.sendChannelMessageDisconnected(
      event.getPlayer().getUuid(),
      this.management.ownNetworkServiceInfo());
    // update the service info
    Wrapper.instance().publishServiceInfoUpdate();
  }
}
