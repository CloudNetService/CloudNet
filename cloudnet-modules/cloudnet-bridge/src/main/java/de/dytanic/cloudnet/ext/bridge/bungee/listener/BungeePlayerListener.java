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

package de.dytanic.cloudnet.ext.bridge.bungee.listener;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeePlayerListener implements Listener {

  private final BungeeCloudNetBridgePlugin plugin;

  public BungeePlayerListener(BungeeCloudNetBridgePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void handle(LoginEvent event) {
    String kickReason = BridgeHelper
      .sendChannelMessageProxyLoginRequest(BungeeCloudNetHelper.createNetworkConnectionInfo(event.getConnection()));
    if (kickReason != null) {
      event.setCancelled(true);
      event.setCancelReason(TextComponent.fromLegacyText(kickReason));
    }
  }

  @EventHandler
  public void handle(ServerSwitchEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(event.getPlayer().getServer().getInfo().getName());

    if (serviceInfoSnapshot != null) {
      BridgeHelper.sendChannelMessageProxyServerSwitch(
        BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()),
        BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
      );
    }
  }

  @EventHandler
  public void handle(ServerConnectEvent event) {
    ProxiedPlayer proxiedPlayer = event.getPlayer();

    // handle the player connection if it is the initial connect to the proxy
    if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
      BridgeHelper.sendChannelMessageProxyLoginSuccess(
        BungeeCloudNetHelper.createNetworkConnectionInfo(proxiedPlayer.getPendingConnection()));
      BridgeHelper.updateServiceInfo();
    }

    ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(event.getTarget().getName());

    if (serviceInfoSnapshot != null) {
      BridgeHelper.sendChannelMessageProxyServerConnectRequest(
        BungeeCloudNetHelper.createNetworkConnectionInfo(proxiedPlayer.getPendingConnection()),
        BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
      );
    }
  }

  @EventHandler
  public void handle(ServerKickEvent event) {
    if (event.getPlayer().isConnected()) {
      ServerInfo kickFrom = event.getKickedFrom();

      if (kickFrom == null) {
        event.getPlayer().disconnect(event.getKickReasonComponent());
        event.setCancelled(true);
        return;
      }

      BridgeProxyHelper.handleConnectionFailed(event.getPlayer().getUniqueId(), kickFrom.getName());
      BungeeCloudNetHelper.getNextFallback(event.getPlayer(), kickFrom).ifPresent(serverInfo -> {
        event.setCancelled(true);
        event.setCancelServer(serverInfo);
        event.getPlayer().sendMessage(event.getKickReasonComponent());
      });
    }
  }

  @EventHandler
  public void handle(PlayerDisconnectEvent event) {
    ProxyServer.getInstance().getScheduler().schedule(this.plugin, () -> {
      BridgeHelper.updateServiceInfo();

      BridgeHelper.sendChannelMessageProxyDisconnect(BungeeCloudNetHelper.createNetworkConnectionInfo(
        event.getPlayer().getPendingConnection()));
      BridgeProxyHelper.clearFallbackProfile(event.getPlayer().getUniqueId());
    }, 50, TimeUnit.MILLISECONDS);
  }
}
