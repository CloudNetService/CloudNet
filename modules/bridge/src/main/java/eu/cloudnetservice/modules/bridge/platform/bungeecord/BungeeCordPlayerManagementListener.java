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

package eu.cloudnetservice.modules.bridge.platform.bungeecord;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.helper.ProxyPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.modules.bridge.util.BridgeHostAndPortUtil;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public final class BungeeCordPlayerManagementListener implements Listener {

  private final Plugin plugin;
  private final PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management;

  public BungeeCordPlayerManagementListener(
    @NonNull Plugin plugin,
    @NonNull PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management
  ) {
    this.plugin = plugin;
    this.management = management;
  }

  @EventHandler
  public void handle(@NonNull LoginEvent event) {
    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      // we need to wrap the proxied player to allow permission checks
      ProxiedPlayer player = new PendingConnectionProxiedPlayer(event.getConnection());
      // check if maintenance is activated
      if (task.maintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
        event.setCancelled(true);
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-maintenance",
          BungeeCordHelper::translateToComponent,
          event::setCancelReason);
        return;
      }
      // check if a custom permission is required to join
      var permission = task.properties().getString("requiredPermission");
      if (permission != null && !player.hasPermission(permission)) {
        event.setCancelled(true);
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-permission",
          BungeeCordHelper::translateToComponent,
          event::setCancelReason);
        return;
      }
    }
    // check if the player is allowed to log in
    var loginResult = ProxyPlatformHelper.sendChannelMessagePreLogin(new NetworkPlayerProxyInfo(
      event.getConnection().getUniqueId(),
      event.getConnection().getName(),
      null,
      event.getConnection().getVersion(),
      BridgeHostAndPortUtil.fromSocketAddress(event.getConnection().getSocketAddress()),
      BridgeHostAndPortUtil.fromSocketAddress(event.getConnection().getListener().getSocketAddress()),
      event.getConnection().isOnlineMode(),
      this.management.ownNetworkServiceInfo()));
    if (!loginResult.permitLogin()) {
      event.setCancelled(true);
      event.setCancelReason(TextComponent.fromLegacyText(legacySection().serialize(loginResult.result())));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(@NonNull ServerConnectEvent event) {
    // initial connect reasons, LOBBY_FALLBACK will be used if the initial fallback is not present
    if (event.getReason() == Reason.JOIN_PROXY || event.getReason() == Reason.LOBBY_FALLBACK) {
      var target = this.management.fallback(event.getPlayer())
        .map(service -> ProxyServer.getInstance().getServerInfo(service.name()))
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

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(@NonNull ServerKickEvent event) {
    if (event.getPlayer().isConnected()) {
      var target = this.management.fallback(event.getPlayer(), event.getKickedFrom().getName())
        .map(service -> ProxyServer.getInstance().getServerInfo(service.name()))
        .orElse(null);
      // check if the server is present
      if (target != null) {
        // reset the fallback profile of the player when he gets kicked while connecting to a server and should get send
        // to the current server. This will not trigger a ServerConnectedEvent which causes incorrect results on the
        // next fallback search
        var curServer = event.getPlayer().getServer();
        if (event.getState() == ServerKickEvent.State.CONNECTING
          && curServer != null
          && curServer.getInfo().equals(target)) {
          this.management.handleFallbackConnectionSuccess(event.getPlayer());
        }

        // we need to cancel the event + set the target server, even when connecting to the same server... Bungee...
        event.setCancelled(true);
        event.setCancelServer(target);

        // extract the reason for the disconnect and wrap it
        this.management.configuration().handleMessage(
          event.getPlayer().getLocale(),
          "error-connecting-to-server",
          message -> BungeeCordHelper.translateToComponent(message
            .replace("%server%", event.getKickedFrom().getName())
            .replace("%reason%", BaseComponent.toLegacyText(event.getKickReasonComponent()))),
          event.getPlayer()::sendMessage);
      } else {
        // no lobby server - the player will disconnect
        event.setCancelled(false);
        event.setCancelServer(null);

        // set the cancel reason
        this.management.configuration().handleMessage(
          event.getPlayer().getLocale(),
          "proxy-join-disconnect-because-no-hub",
          BungeeCordHelper::translateToComponent,
          event::setKickReasonComponent);
      }
    }
  }

  @EventHandler
  public void handle(@NonNull ServerConnectedEvent event) {
    var joinedServiceInfo = this.management
      .cachedService(service -> service.name().equals(event.getServer().getInfo().getName()))
      .map(NetworkServiceInfo::fromServiceInfoSnapshot)
      .orElse(null);
    // check if the player connection was initial
    if (event.getPlayer().getServer() == null) {
      ProxyPlatformHelper.sendChannelMessageLoginSuccess(
        this.management.createPlayerInformation(event.getPlayer()),
        joinedServiceInfo);
      // update the service info
      Wrapper.instance().publishServiceInfoUpdate();
    } else if (joinedServiceInfo != null) {
      // the player switched the service
      ProxyPlatformHelper.sendChannelMessageServiceSwitch(event.getPlayer().getUniqueId(), joinedServiceInfo);
    }
    // publish the player connection to the handler
    this.management.handleFallbackConnectionSuccess(event.getPlayer());
  }

  @EventHandler
  public void handle(@NonNull PlayerDisconnectEvent event) {
    // check if the player was connected to a server before
    if (event.getPlayer().getServer() != null) {
      ProxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      // update the service info
      ProxyServer.getInstance().getScheduler().schedule(
        this.plugin,
        Wrapper.instance()::publishServiceInfoUpdate,
        50,
        TimeUnit.MILLISECONDS);
    }
    // always remove the player fallback profile
    this.management.removeFallbackProfile(event.getPlayer());
  }
}
