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

import com.velocitypowered.api.event.PostOrder;
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
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;

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

  @Subscribe
  public void handleLogin(@NonNull LoginEvent event) {
    var task = this.management.selfTask();
    // check if the current task is present
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
    var loginResult = this.proxyPlatformHelper.sendChannelMessagePreLogin(
      this.management.createPlayerInformation(event.getPlayer()));
    if (!loginResult.permitLogin()) {
      event.setResult(ResultedEvent.ComponentResult.denied(loginResult.result()));
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  public void handleInitialServerChoose(@NonNull PlayerChooseInitialServerEvent event) {
    // filter the next fallback
    event.setInitialServer(this.management.fallback(event.getPlayer())
      .flatMap(service -> this.proxyServer.getServer(service.name()))
      .orElse(null));
  }

  @Subscribe(order = PostOrder.FIRST)
  public void handleServerKick(@NonNull KickedFromServerEvent event) {
    // check if the player is still active
    if (event.getPlayer().isActive()) {
      event.setResult(this.management.fallback(event.getPlayer(), event.getServer().getServerInfo().getName())
        .flatMap(service -> this.proxyServer.getServer(service.name()))
        .map(server -> {
          // only notify the player if the fallback is the server the player is connected to
          var curServer = event.getPlayer().getCurrentServer().map(ServerConnection::getServerInfo).orElse(null);
          if (event.kickedDuringServerConnect() && curServer != null && curServer.equals(server.getServerInfo())) {
            // send the player a nice message - velocity will keep the connection to the current server
            // therefore we need to reset the fallback profile as no ServerPostConnectEvent will be called
            this.management.handleFallbackConnectionSuccess(event.getPlayer());
            return KickedFromServerEvent.Notify.create(this.extractReasonComponent(event));
          } else {
            // redirect the player to the next available hub server
            return KickedFromServerEvent.RedirectPlayer.create(server, this.extractReasonComponent(event));
          }
        })
        .orElse(KickedFromServerEvent.DisconnectPlayer.create(this.management.configuration().findMessage(
          event.getPlayer().getEffectiveLocale(),
          "proxy-join-disconnect-because-no-hub",
          ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
          Component.empty(),
          true))));
    }
  }

  @Subscribe
  public void handleServiceConnected(@NonNull ServerPostConnectEvent event) {
    var joinedServiceInfo = event.getPlayer().getCurrentServer()
      .flatMap(server -> this.management
        .cachedService(service -> server.getServerInfo().getName().equals(service.name()))
        .map(NetworkServiceInfo::fromServiceInfoSnapshot))
      .orElse(null);
    // check if the connection was initial
    if (event.getPreviousServer() == null) {
      // the player logged in successfully if he is now connected to a service for the first time
      this.proxyPlatformHelper.sendChannelMessageLoginSuccess(
        this.management.createPlayerInformation(event.getPlayer()),
        joinedServiceInfo);
      // update the service info
      this.serviceInfoHolder.publishServiceInfoUpdate();
    } else if (joinedServiceInfo != null) {
      // the player switched the service
      this.proxyPlatformHelper.sendChannelMessageServiceSwitch(event.getPlayer().getUniqueId(), joinedServiceInfo);
    }
    // notify the management that the player successfully connected to a service
    this.management.handleFallbackConnectionSuccess(event.getPlayer());
  }

  @Subscribe
  public void handleDisconnect(@NonNull DisconnectEvent event) {
    // check if the player successfully connected to a server before
    // PRE_SERVER_JOIN will be used when the upstream server closes the connection to the player, we need to handle this
    var status = event.getLoginStatus();
    if (status == DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN
      || status == DisconnectEvent.LoginStatus.PRE_SERVER_JOIN) {
      this.proxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      // update the service info
      this.serviceInfoHolder.publishServiceInfoUpdate();
    }
    // always remove the player fallback profile
    this.management.removeFallbackProfile(event.getPlayer());
  }

  private @NonNull Component extractReasonComponent(@NonNull KickedFromServerEvent event) {
    var playerLocale = event.getPlayer().getEffectiveLocale();
    var message = event.getServerKickReason().orElse(null);
    // use the current result if it is Notify - velocity already created a friendly reason for us
    if (message == null && event.getResult() instanceof KickedFromServerEvent.Notify notify) {
      message = notify.getMessageComponent();
    }
    // check if we have a reason component
    if (message != null) {
      // check if we should try to translate the message
      if (message instanceof TranslatableComponent) {
        message = GlobalTranslator.render(message, playerLocale == null ? Locale.getDefault() : playerLocale);
      }
      // if the message is still not a TextComponent use the default message instead
      if (message instanceof TextComponent textComponent) {
        // wrap the message
        return this.management.configuration().findMessage(
          playerLocale,
          "error-connecting-to-server",
          rawMessage -> ComponentFormats.BUNGEE_TO_ADVENTURE.convert(rawMessage
            .replace("%server%", event.getServer().getServerInfo().getName())
            .replace("%reason%", LegacyComponentSerializer.legacySection().serialize(textComponent))),
          Component.empty(),
          true);
      }
    }

    // unknown reason
    return this.management.configuration().findMessage(
      playerLocale,
      "error-connecting-to-server",
      rawMessage -> ComponentFormats.BUNGEE_TO_ADVENTURE.convert(rawMessage
        .replace("%server%", event.getServer().getServerInfo().getName())
        .replace("%reason%", "§cUnknown")),
      Component.empty(),
      true);
  }
}
