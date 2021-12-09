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

package de.dytanic.cloudnet.ext.bridge.platform.bungeecord;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.node.event.LocalPlayerPreLoginEvent.Result;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.helper.ProxyPlatformHelper;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

final class BungeeCordPlayerManagementListener implements Listener {

  private final Plugin plugin;
  private final PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management;

  public BungeeCordPlayerManagementListener(
    @NotNull Plugin plugin,
    @NotNull PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management
  ) {
    this.plugin = plugin;
    this.management = management;
  }

  @EventHandler
  public void handle(@NotNull LoginEvent event) {
    ServiceTask task = this.management.getSelfTask();
    // check if the current task is present
    if (task != null) {
      // we need to wrap the proxied player to allow permission checks
      ProxiedPlayer player = new PendingConnectionProxiedPlayer(event.getConnection());
      // check if maintenance is activated
      if (task.isMaintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
        event.setCancelled(true);
        event.setCancelReason(TextComponent.fromLegacyText(this.management.getConfiguration().getMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-maintenance")));
        return;
      }
      // check if a custom permission is required to join
      String permission = task.getProperties().getString("requiredPermission");
      if (permission != null && !player.hasPermission(permission)) {
        event.setCancelled(true);
        event.setCancelReason(TextComponent.fromLegacyText(this.management.getConfiguration().getMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-permission")));
        return;
      }
    }
    // check if the player is allowed to log in
    Result loginResult = ProxyPlatformHelper.sendChannelMessagePreLogin(new NetworkPlayerProxyInfo(
      event.getConnection().getUniqueId(),
      event.getConnection().getName(),
      null,
      event.getConnection().getVersion(),
      new HostAndPort((InetSocketAddress) event.getConnection().getSocketAddress()),
      new HostAndPort((InetSocketAddress) event.getConnection().getListener().getSocketAddress()),
      event.getConnection().isOnlineMode(),
      this.management.getOwnNetworkServiceInfo()));
    if (!loginResult.isAllowed()) {
      event.setCancelled(true);
      event.setCancelReason(TextComponent.fromLegacyText(legacySection().serialize(loginResult.getResult())));
    }
  }

  @EventHandler
  public void handle(@NotNull ServerConnectEvent event) {
    // initial connect reasons, LOBBY_FALLBACK will be used if the initial fallback is not present
    if (event.getReason() == Reason.JOIN_PROXY || event.getReason() == Reason.LOBBY_FALLBACK) {
      ServerInfo target = this.management.getFallback(event.getPlayer())
        .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()))
        .orElse(null);
      // check if the server is present
      if (target != null) {
        event.setTarget(target);
      } else {
        // no lobby server - cancel the event; another plugin might be able to override that decision
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void handle(@NotNull ServerKickEvent event) {
    if (event.getPlayer().isConnected()) {
      ServerInfo target = this.management.getFallback(event.getPlayer(), event.getKickedFrom().getName())
        .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()))
        .orElse(null);
      // check if the server is present
      if (target != null) {
        event.setCancelled(true);
        event.setCancelServer(target);
        // extract the reason for the disconnect and wrap it
        Locale playerLocale = event.getPlayer().getLocale();
        String baseMessage = this.management.getConfiguration().getMessage(playerLocale, "error-connecting-to-server")
          .replace("%server%", event.getKickedFrom().getName())
          .replace("%reason%", ComponentSerializer.toString(event.getKickReasonComponent()));
        // send the player the reason for the disconnect
        event.getPlayer().sendMessage(TextComponent.fromLegacyText(baseMessage));
      } else {
        // no lobby server - the player will disconnect
        event.setCancelled(false);
        event.setCancelServer(null);
        event.setKickReasonComponent(TextComponent.fromLegacyText(this.management.getConfiguration().getMessage(
          event.getPlayer().getLocale(),
          "proxy-join-disconnect-because-no-hub")));
      }
    }
  }

  @EventHandler
  public void handle(@NotNull ServerConnectedEvent event) {
    // check if the player connection was initial
    if (event.getPlayer().getServer() == null) {
      ProxyPlatformHelper.sendChannelMessageLoginSuccess(this.management.createPlayerInformation(event.getPlayer()));
      // update the service info
      Wrapper.getInstance().publishServiceInfoUpdate();
    } else {
      // server switch
      // the player switched the service
      this.management
        .getCachedService(service -> service.getName().equals(event.getServer().getInfo().getName()))
        .map(BridgeServiceHelper::createServiceInfo)
        .ifPresent(info -> ProxyPlatformHelper.sendChannelMessageServiceSwitch(event.getPlayer().getUniqueId(), info));
    }
    // publish the player connection to the handler
    this.management.handleFallbackConnectionSuccess(event.getPlayer());
  }

  @EventHandler
  public void handle(@NotNull PlayerDisconnectEvent event) {
    // check if the player was connected to a server before
    if (event.getPlayer().getServer() != null) {
      ProxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      // update the service info
      ProxyServer.getInstance().getScheduler().schedule(
        this.plugin,
        Wrapper.getInstance()::publishServiceInfoUpdate,
        50,
        TimeUnit.MILLISECONDS);
    }
    // always remove the player fallback profile
    this.management.removeFallbackProfile(event.getPlayer());
  }
}
