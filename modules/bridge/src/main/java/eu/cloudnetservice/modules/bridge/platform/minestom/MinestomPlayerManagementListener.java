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

package eu.cloudnetservice.modules.bridge.platform.minestom;

import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.helper.ServerPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

@Singleton
public final class MinestomPlayerManagementListener {

  private final ServiceInfoHolder serviceInfoHolder;
  private final ServerPlatformHelper serverPlatformHelper;
  private final PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management;

  @Inject
  public MinestomPlayerManagementListener(
    @NonNull GlobalEventHandler eventHandler,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull ServerPlatformHelper serverPlatformHelper,
    @NonNull PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management
  ) {
    this.serviceInfoHolder = serviceInfoHolder;
    this.serverPlatformHelper = serverPlatformHelper;
    this.management = management;
    // listen on these events and redirect them into the methods
    var node = EventNode.type("cloudnet-bridge", EventFilter.PLAYER);
    eventHandler.addChild(node
      .addListener(AsyncPlayerPreLoginEvent.class, this::handleLogin)
      .addListener(PlayerSpawnEvent.class, this::handleJoin)
      .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));
  }

  private void handleLogin(@NonNull AsyncPlayerPreLoginEvent event) {
    var player = event.getPlayer();
    // ignore fake players
    if (player instanceof FakePlayer) {
      return;
    }

    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.maintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
        this.management.configuration().handleMessage(
          player.getLocale(),
          "server-join-cancel-because-maintenance",
          ComponentFormats.ADVENTURE,
          player::kick);
        return;
      }
      // check if a custom permission is required to join
      var permission = task.propertyHolder().getString("requiredPermission");
      if (permission != null && !player.hasPermission(permission)) {
        this.management.configuration().handleMessage(
          player.getLocale(),
          "server-join-cancel-because-permission",
          ComponentFormats.ADVENTURE,
          player::kick);
      }
    }
  }

  private void handleJoin(@NonNull PlayerSpawnEvent event) {
    // ignore fake players
    if (event.getPlayer() instanceof FakePlayer) {
      return;
    }

    this.serverPlatformHelper.sendChannelMessageLoginSuccess(
      event.getPlayer().getUuid(),
      this.management.createPlayerInformation(event.getPlayer()));
    // update the service info
    this.serviceInfoHolder.publishServiceInfoUpdate();
  }

  private void handleDisconnect(@NonNull PlayerDisconnectEvent event) {
    // ignore fake players
    if (event.getPlayer() instanceof FakePlayer) {
      return;
    }

    this.serverPlatformHelper.sendChannelMessageDisconnected(
      event.getPlayer().getUuid(),
      this.management.ownNetworkServiceInfo());
    // update the service info
    this.serviceInfoHolder.publishServiceInfoUpdate();
  }
}
