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
import eu.cloudnetservice.ext.bungee.BungeeComponentUtil;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.helper.ProxyPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
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

  // placeholders when creating reason messages
  private static final Pattern KICK_REASON = Pattern.compile("%reason%", Pattern.LITERAL);
  private static final Pattern SERVER_NAME = Pattern.compile("%server%", Pattern.LITERAL);

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

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handle(@NonNull ServerConnectEvent event) {
    // check if another plugin already prevented the connection process
    if (event.isCancelled()) {
      return;
    }

    var player = event.getPlayer();
    if (event.getReason() == Reason.JOIN_PROXY) {
      var task = this.management.selfTask();
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
      var playerInfo = this.management.createPlayerInformation(player);
      var loginResult = this.proxyPlatformHelper.sendChannelMessagePreLogin(playerInfo);
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
      if (target != null) {
        // fallback found, connect to the fallback
        event.setTarget(target);
      } else {
        // no fallback found, disconnect the player
        event.setCancelled(true);
        var kickMessage = this.management.configuration().findMessage(
          player.getLocale(),
          "proxy-join-disconnect-because-no-hub",
          ComponentFormats.ADVENTURE_TO_BUNGEE::convert,
          null,
          true);
        if (kickMessage != null) {
          player.disconnect(kickMessage);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handleEarlyKick(@NonNull ServerKickEvent event) {
    var player = event.getPlayer();
    if (player.isConnected()) {
      var fallback = this.management.fallback(player, event.getKickedFrom().getName())
        .map(service -> this.proxyServer.getServerInfo(service.name()))
        .orElse(null);
      if (fallback != null) {
        // reset the fallback profile of the player when he gets kicked while connecting to a server and should get send
        // to the current server. This will not trigger a ServerConnectedEvent which causes incorrect results on the
        // next fallback search
        var prevServer = player.getServer();
        if (event.getState() == ServerKickEvent.State.CONNECTING
          && prevServer != null
          && prevServer.getInfo().equals(fallback)) {
          this.management.handleFallbackConnectionSuccess(player);
        }

        // we need to cancel the event + set the target server, even when connecting to the same server... Bungee...
        event.setCancelled(true);
        event.setCancelServer(fallback);

        // extract the reason for the disconnect and wrap it
        var kickReason = this.buildKickReasonMessage(
          player,
          event.getKickedFrom(),
          event.getReason(),
          "error-connecting-to-server");
        player.sendMessage(kickReason);
      } else {
        // no fallback available that the player can connect to
        event.setCancelled(false);
        event.setCancelServer(null);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handleLateKick(@NonNull ServerKickEvent event) {
    // early kick handling selects the fallback, this late kick handling provides
    // the correct kick reason as the reason cannot be set otherwise (bungeecord would
    // insert a prefix into the message that we don't want)
    var player = event.getPlayer();
    if (player.isConnected() && (!event.isCancelled() || event.getCancelServer() == null)) {
      var kickReason = this.buildKickReasonMessage(
        player,
        event.getKickedFrom(),
        event.getReason(),
        "server-kick-no-other-hub");
      player.disconnect(kickReason);
    }
  }

  @EventHandler
  public void handle(@NonNull ServerConnectedEvent event) {
    var player = event.getPlayer();
    var joinedServiceInfo = this.management
      .cachedService(service -> service.name().equals(event.getServer().getInfo().getName()))
      .map(NetworkServiceInfo::fromServiceInfoSnapshot)
      .orElse(null);
    if (this.initialConnect(player)) {
      // the player logged in successfully if he is now connected to a service for the first time
      var playerInfo = this.management.createPlayerInformation(player);
      this.proxyPlatformHelper.sendChannelMessageLoginSuccess(playerInfo, joinedServiceInfo);
      this.serviceInfoHolder.publishServiceInfoUpdate();
    } else if (joinedServiceInfo != null) {
      // the player switched the service
      this.proxyPlatformHelper.sendChannelMessageServiceSwitch(player.getUniqueId(), joinedServiceInfo);
    }

    this.management.handleFallbackConnectionSuccess(player);
  }

  @EventHandler
  public void handle(@NonNull PlayerDisconnectEvent event) {
    // check if the player was connected to a server before
    var player = event.getPlayer();
    if (player.getServer() != null) {
      this.proxyPlatformHelper.sendChannelMessageDisconnected(player.getUniqueId());
      this.scheduler.schedule(this.plugin, this.serviceInfoHolder::publishServiceInfoUpdate, 50, TimeUnit.MILLISECONDS);
    }

    this.management.removeFallbackProfile(player);
  }

  private boolean initialConnect(@NonNull ProxiedPlayer player) {
    // since 1.20.2 we can not detect an initial connect using the nullability of the server field
    // but the dimension of a player is null on an initial connect and non-null on a server switch
    if (player.getPendingConnection().getVersion() >= PROTOCOL_1_20_2) {
      return PLAYER_DIMENSION_ACCESSOR.invoke(player).getOrThrow() == null;
    }

    return player.getServer() == null;
  }

  private @NonNull BaseComponent buildKickReasonMessage(
    @NonNull ProxiedPlayer player,
    @NonNull ServerInfo kickedFrom,
    @NonNull BaseComponent kickReason,
    @NonNull String messageKey
  ) {
    var rawReasonMessage = this.management.configuration().findMessage(
      player.getLocale(),
      messageKey,
      ComponentFormats.ADVENTURE_TO_BUNGEE::convert,
      null,
      true);
    if (rawReasonMessage == null) {
      // if no message is configured in the bridge config, fall back to the kick
      // reason provided by the server, as we do need a message to prevent a
      // possible disconnection by the velocity
      return kickReason;
    } else {
      var component = rawReasonMessage.length == 1 ? rawReasonMessage[0] : new TextComponent(rawReasonMessage);
      component = BungeeComponentUtil.replaceText(component, KICK_REASON, kickReason);
      component = BungeeComponentUtil.replaceText(component, SERVER_NAME, new TextComponent(kickedFrom.getName()));
      return component;
    }
  }
}
