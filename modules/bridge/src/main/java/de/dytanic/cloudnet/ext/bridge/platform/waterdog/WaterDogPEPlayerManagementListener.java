/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.bridge.platform.waterdog;

import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.helper.ProxyPlatformHelper;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.TransferCompleteEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import java.util.Locale;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

final class WaterDogPEPlayerManagementListener {

  private final PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management;

  public WaterDogPEPlayerManagementListener(
    @NotNull PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management
  ) {
    this.management = management;
    // subscribe to all events
    ProxyServer.getInstance().getEventManager().subscribe(PlayerLoginEvent.class, this::handleLogin);
    ProxyServer.getInstance().getEventManager().subscribe(TransferCompleteEvent.class, this::handleTransfer);
    ProxyServer.getInstance().getEventManager().subscribe(PlayerDisconnectEvent.class, this::handleDisconnected);
  }

  private void handleLogin(@NotNull PlayerLoginEvent event) {
    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.maintenance() && !event.getPlayer().hasPermission("cloudnet.bridge.maintenance")) {
        event.setCancelled(true);
        event.setCancelReason(this.management.configuration().message(
          Locale.ENGLISH,
          "proxy-join-cancel-because-maintenance"));
        return;
      }
      // check if a custom permission is required to join
      var permission = task.properties().getString("requiredPermission");
      if (permission != null && !event.getPlayer().hasPermission(permission)) {
        event.setCancelled(true);
        event.setCancelReason(this.management.configuration().message(
          Locale.ENGLISH,
          "proxy-join-cancel-because-permission"));
        return;
      }
    }
    // check if the player is allowed to log in
    var loginResult = ProxyPlatformHelper.sendChannelMessagePreLogin(
      this.management.createPlayerInformation(event.getPlayer()));
    if (!loginResult.permitLogin()) {
      event.setCancelled(true);
      event.setCancelReason(LegacyComponentSerializer.legacySection().serialize(loginResult.result()));
    }
  }

  private void handleTransfer(@NotNull TransferCompleteEvent event) {
    if (event.getOldClient() == null) {
      // the player logged in successfully if he is now connected to a service for the first time
      ProxyPlatformHelper.sendChannelMessageLoginSuccess(this.management.createPlayerInformation(event.getPlayer()));
      // update the service info
      Wrapper.instance().publishServiceInfoUpdate();
    } else {
      // the player switched the service
      this.management
        .cachedService(service -> service.name().equals(event.getNewClient().getServerInfo().getServerName()))
        .map(BridgeServiceHelper::createServiceInfo)
        .ifPresent(info -> ProxyPlatformHelper.sendChannelMessageServiceSwitch(event.getPlayer().getUniqueId(), info));
    }
    // notify the management that the player successfully connected to a service
    this.management.handleFallbackConnectionSuccess(event.getPlayer());
  }

  private void handleDisconnected(@NotNull PlayerDisconnectEvent event) {
    // check if the player successfully connected to a server before
    if (event.getPlayer().getServerInfo() != null) {
      ProxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      // update the service info
      ProxyServer.getInstance().getScheduler().scheduleDelayed(Wrapper.instance()::publishServiceInfoUpdate, 1);
    }
    // always remove the player fallback profile
    this.management.removeFallbackProfile(event.getPlayer());
  }
}
