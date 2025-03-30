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

package eu.cloudnetservice.modules.bridge.impl.platform.velocity;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.helper.ProxyPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Locale;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

@Singleton
public final class VelocityPlayerManagementListener {

  private final ProxyServer proxyServer;
  private final ServiceInfoHolder serviceInfoHolder;
  private final ProxyPlatformHelper proxyPlatformHelper;
  private final PlatformBridgeManagement<Player, NetworkPlayerProxyInfo> management;

  @Inject
  public VelocityPlayerManagementListener(
    @NonNull ProxyServer proxyServer,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull ProxyPlatformHelper proxyPlatformHelper,
    @NonNull PlatformBridgeManagement<Player, NetworkPlayerProxyInfo> management
  ) {
    this.proxyServer = proxyServer;
    this.serviceInfoHolder = serviceInfoHolder;
    this.proxyPlatformHelper = proxyPlatformHelper;
    this.management = management;
  }

  @Subscribe(priority = Short.MIN_VALUE)
  public void handleLogin(@NonNull LoginEvent event) {
    var task = this.management.selfTask();
    if (task != null) {
      // check if maintenance is activated
      if (task.maintenance() && !event.getPlayer().hasPermission("cloudnet.bridge.maintenance")) {
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-maintenance",
          ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
          component -> event.setResult(ResultedEvent.ComponentResult.denied(component)));
        return;
      }

      // check if a custom permission is required to join
      var permission = task.propertyHolder().getString("requiredPermission");
      if (permission != null && !event.getPlayer().hasPermission(permission)) {
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-permission",
          ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
          component -> event.setResult(ResultedEvent.ComponentResult.denied(component)));
        return;
      }
    }

    // check if the player is allowed to log in
    var playerInfo = this.management.createPlayerInformation(event.getPlayer());
    var loginResult = this.proxyPlatformHelper.sendChannelMessagePreLogin(playerInfo);
    if (!loginResult.permitLogin()) {
      event.setResult(ResultedEvent.ComponentResult.denied(loginResult.result()));
    }
  }

  @Subscribe(priority = Short.MAX_VALUE)
  public void handleInitialServerChoose(@NonNull PlayerChooseInitialServerEvent event) {
    this.management.fallback(event.getPlayer())
      .flatMap(service -> this.proxyServer.getServer(service.name()))
      .ifPresentOrElse(event::setInitialServer, () -> {
        var kickMessage = this.management.configuration().findMessage(
          event.getPlayer().getEffectiveLocale(),
          "proxy-join-disconnect-because-no-hub",
          ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
          null,
          true);
        if (kickMessage != null) {
          event.getPlayer().disconnect(kickMessage);
        }
      });
  }

  @Subscribe(priority = Short.MAX_VALUE)
  public void handleServerKick(@NonNull KickedFromServerEvent event) {
    var player = event.getPlayer();
    if (player.isActive()) {
      var kickResult = this.management.fallback(player, event.getServer().getServerInfo().getName())
        .flatMap(service -> this.proxyServer.getServer(service.name()))
        .map(fallback -> {
          var kickReason = this.buildKickReasonMessage(event, "error-connecting-to-server");
          var prevServer = player.getCurrentServer().map(ServerConnection::getServerInfo).orElse(null);
          if (event.kickedDuringServerConnect() && prevServer != null && prevServer.equals(fallback.getServerInfo())) {
            // send the player a nice message - velocity will keep the connection to the current server
            // therefore we need to reset the fallback profile as no ServerPostConnectEvent will be called
            this.management.handleFallbackConnectionSuccess(player);
            return KickedFromServerEvent.Notify.create(kickReason);
          } else {
            // redirect the player to the next available hub server
            return KickedFromServerEvent.RedirectPlayer.create(fallback, kickReason);
          }
        })
        .orElseGet(() -> {
          // no fallback available, disconnect the player
          var kickReason = this.buildKickReasonMessage(event, "server-kick-no-other-hub");
          return KickedFromServerEvent.DisconnectPlayer.create(kickReason);
        });
      event.setResult(kickResult);
    }
  }

  @Subscribe
  public void handleServiceConnected(@NonNull ServerPostConnectEvent event) {
    var player = event.getPlayer();
    var joinedServiceInfo = player.getCurrentServer()
      .flatMap(server -> this.management
        .cachedService(service -> server.getServerInfo().getName().equals(service.name()))
        .map(NetworkServiceInfo::fromServiceInfoSnapshot))
      .orElse(null);
    if (event.getPreviousServer() == null) {
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

  @Subscribe
  public void handleDisconnect(@NonNull DisconnectEvent event) {
    // check if the player successfully connected to a server before
    // PRE_SERVER_JOIN will be used when the upstream server closes the connection to the player, we need to handle this
    var status = event.getLoginStatus();
    if (status == DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN
      || status == DisconnectEvent.LoginStatus.PRE_SERVER_JOIN) {
      this.proxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      this.serviceInfoHolder.publishServiceInfoUpdate();
    }

    this.management.removeFallbackProfile(event.getPlayer());
  }

  private @NonNull Component buildKickReasonMessage(@NonNull KickedFromServerEvent event, @NonNull String messageKey) {
    var playerLocale = event.getPlayer().getEffectiveLocale();
    var serverName = event.getServer().getServerInfo().getName();
    var reason = event.getServerKickReason().orElseGet(() -> Component.text("Disconnected by Server"));

    var rawReasonMessage = this.management.configuration().findMessage(
      playerLocale,
      messageKey,
      ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
      null,
      true);
    if (rawReasonMessage == null) {
      // if no message is configured in the bridge config, fall back to the kick
      // reason provided by the server, as we do need a message to prevent a
      // possible disconnection by the velocity
      return reason;
    } else {
      return rawReasonMessage
        .replaceText(builder -> builder.matchLiteral("%reason%").replacement(reason))
        .replaceText(builder -> builder.matchLiteral("%server%").replacement(serverName));
    }
  }
}
