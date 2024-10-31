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

package eu.cloudnetservice.modules.bridge.impl.platform.bungeecord;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.helper.ProxyPlatformHelper;
import eu.cloudnetservice.modules.bridge.impl.util.BridgeHostAndPortUtil;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

@Singleton
public final class BungeeCordPlayerManagementListener implements Listener {

  // https://minecraft.wiki/w/Java_Edition_1.20.2
  // 1.20.2 changed the login process and therefore BungeeCord is somewhat breaking
  private static final int PROTOCOL_1_20_2 = 764;
  private static final MethodAccessor<?> PLAYER_DIMENSION_ACCESSOR = Reflexion.get(
      "net.md_5.bungee.UserConnection",
      null)
    .findMethod("getDimension")
    .orElseThrow();

  private final Plugin plugin;
  private final ProxyServer proxyServer;
  private final TaskScheduler scheduler;
  private final ServiceInfoHolder serviceInfoHolder;
  private final ProxyPlatformHelper proxyPlatformHelper;
  private final PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management;

  @Inject
  public BungeeCordPlayerManagementListener(
    @NonNull Plugin plugin,
    @NonNull ProxyServer proxyServer,
    @NonNull TaskScheduler scheduler,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull ProxyPlatformHelper proxyPlatformHelper,
    @NonNull PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management
  ) {
    this.plugin = plugin;
    this.proxyServer = proxyServer;
    this.scheduler = scheduler;
    this.serviceInfoHolder = serviceInfoHolder;
    this.proxyPlatformHelper = proxyPlatformHelper;
    this.management = management;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(@NonNull ServerConnectEvent event) {
    var player = event.getPlayer();
    // handle permission checks here before we send a request to the node about the login
    if (event.getReason() == Reason.JOIN_PROXY) {
      var task = this.management.selfTask();
      // check if the current task is present
      if (task != null) {
        // check if maintenance is activated
        if (task.maintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
          event.setCancelled(true);
          this.management.configuration().handleMessage(
            player.getLocale(),
            "proxy-join-cancel-because-maintenance",
            ComponentFormats.ADVENTURE_TO_BUNGEE::convert,
            player::disconnect);
          return;
        }
        // check if a custom permission is required to join
        var permission = task.propertyHolder().getString("requiredPermission");
        if (permission != null && !player.hasPermission(permission)) {
          event.setCancelled(true);
          this.management.configuration().handleMessage(
            player.getLocale(),
            "proxy-join-cancel-because-permission",
            ComponentFormats.ADVENTURE_TO_BUNGEE::convert,
            player::disconnect);
          return;
        }
      }
      // check if the player is allowed to log in
      var loginResult = this.proxyPlatformHelper.sendChannelMessagePreLogin(new NetworkPlayerProxyInfo(
        player.getUniqueId(),
        player.getName(),
        null,
        player.getPendingConnection().getVersion(),
        BridgeHostAndPortUtil.fromSocketAddress(player.getSocketAddress()),
        BridgeHostAndPortUtil.fromSocketAddress(player.getPendingConnection().getListener().getSocketAddress()),
        player.getPendingConnection().isOnlineMode(),
        this.management.ownNetworkServiceInfo()));
      if (!loginResult.permitLogin()) {
        event.setCancelled(true);
        player.disconnect(TextComponent.fromLegacyText(legacySection().serialize(loginResult.result())));
        return;
      }
    }

    // initial connect reasons, LOBBY_FALLBACK will be used if the initial fallback is not present
    if (event.getReason() == Reason.JOIN_PROXY || event.getReason() == Reason.LOBBY_FALLBACK) {
      var target = this.management.fallback(player)
        .map(service -> this.proxyServer.getServerInfo(service.name()))
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
        .map(service -> this.proxyServer.getServerInfo(service.name()))
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
          message -> ComponentFormats.ADVENTURE_TO_BUNGEE.convert(message
            .replace("%server%", event.getKickedFrom().getName())
            // TODO: take a look at single components instead of arrays
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
          ComponentFormats.ADVENTURE_TO_BUNGEE::convert,
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
    if (this.initialConnect(event.getPlayer())) {
      this.proxyPlatformHelper.sendChannelMessageLoginSuccess(
        this.management.createPlayerInformation(event.getPlayer()),
        joinedServiceInfo);
      // update the service info
      this.serviceInfoHolder.publishServiceInfoUpdate();
    } else if (joinedServiceInfo != null) {
      // the player switched the service
      this.proxyPlatformHelper.sendChannelMessageServiceSwitch(event.getPlayer().getUniqueId(), joinedServiceInfo);
    }
    // publish the player connection to the handler
    this.management.handleFallbackConnectionSuccess(event.getPlayer());
  }

  @EventHandler
  public void handle(@NonNull PlayerDisconnectEvent event) {
    // check if the player was connected to a server before
    if (event.getPlayer().getServer() != null) {
      this.proxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      // update the service info
      this.scheduler.schedule(this.plugin, this.serviceInfoHolder::publishServiceInfoUpdate, 50, TimeUnit.MILLISECONDS);
    }
    // always remove the player fallback profile
    this.management.removeFallbackProfile(event.getPlayer());
  }

  private boolean initialConnect(@NonNull ProxiedPlayer player) {
    // since 1.20.2 we can not detect an initial connect using the nullability of the server field
    // but the dimension of a player is null on an initial connect and non-null on a server switch
    if (player.getPendingConnection().getVersion() >= PROTOCOL_1_20_2) {
      return PLAYER_DIMENSION_ACCESSOR.invoke(player).getOrThrow() == null;
    }

    return player.getServer() == null;
  }

}
