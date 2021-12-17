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

package de.dytanic.cloudnet.ext.bridge.platform.velocity;

import static eu.cloudnetservice.ext.adventure.AdventureSerializerUtil.serialize;

import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.DisconnectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.Notify;
import com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.helper.ProxyPlatformHelper;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Locale;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.NotNull;

final class VelocityPlayerManagementListener {

  private final ProxyServer proxyServer;
  private final PlatformBridgeManagement<Player, NetworkPlayerProxyInfo> management;

  public VelocityPlayerManagementListener(
    @NotNull ProxyServer proxyServer,
    @NotNull PlatformBridgeManagement<Player, NetworkPlayerProxyInfo> management
  ) {
    this.proxyServer = proxyServer;
    this.management = management;
  }

  @Subscribe
  public void handleLogin(@NotNull LoginEvent event) {
    var task = this.management.getSelfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.isMaintenance() && !event.getPlayer().hasPermission("cloudnet.bridge.maintenance")) {
        event.setResult(ComponentResult.denied(serialize(this.management.getConfiguration().getMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-maintenance"))));
        return;
      }
      // check if a custom permission is required to join
      var permission = task.properties().getString("requiredPermission");
      if (permission != null && !event.getPlayer().hasPermission(permission)) {
        event.setResult(ComponentResult.denied(serialize(this.management.getConfiguration().getMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-permission"))));
        return;
      }
    }
    // check if the player is allowed to log in
    var loginResult = ProxyPlatformHelper.sendChannelMessagePreLogin(
      this.management.createPlayerInformation(event.getPlayer()));
    if (!loginResult.isAllowed()) {
      event.setResult(ComponentResult.denied(loginResult.getResult()));
    }
  }

  @Subscribe
  public void handleInitialServerChoose(@NotNull PlayerChooseInitialServerEvent event) {
    // filter the next fallback
    event.setInitialServer(this.management.getFallback(event.getPlayer())
      .flatMap(service -> this.proxyServer.getServer(service.name()))
      .orElse(null));
  }

  @Subscribe
  public void handleServerKick(@NotNull KickedFromServerEvent event) {
    // check if the player is still active
    if (event.getPlayer().isActive()) {
      event.setResult(this.management.getFallback(event.getPlayer(), event.getServer().getServerInfo().getName())
        .flatMap(service -> this.proxyServer.getServer(service.name()))
        .map(server -> {
          // only notify the player if the fallback is the server the player is connected to
          var curServer = event.getPlayer().getCurrentServer().map(ServerConnection::getServerInfo).orElse(null);
          if (event.kickedDuringServerConnect() && curServer != null && curServer.equals(server.getServerInfo())) {
            // send the player a nice message - velocity will keep the connection to the current server
            return Notify.create(this.getReasonComponent(event));
          } else {
            // send the player a reason message
            event.getPlayer().sendMessage(Identity.nil(), this.getReasonComponent(event));
            // redirect the player to the next available hub server
            return RedirectPlayer.create(server);
          }
        })
        .orElse(DisconnectPlayer.create(serialize(this.management.getConfiguration().getMessage(
          event.getPlayer().getEffectiveLocale(),
          "proxy-join-disconnect-because-no-hub")))));
    }
  }

  @Subscribe
  public void handleServiceConnected(@NotNull ServerPostConnectEvent event) {
    if (event.getPreviousServer() == null) {
      // the player logged in successfully if he is now connected to a service for the first time
      ProxyPlatformHelper.sendChannelMessageLoginSuccess(this.management.createPlayerInformation(event.getPlayer()));
      // update the service info
      Wrapper.getInstance().publishServiceInfoUpdate();
    } else {
      // the player switched the service
      event.getPlayer().getCurrentServer()
        .flatMap(server -> this.management
          .getCachedService(service -> server.getServerInfo().getName().equals(service.name()))
          .map(BridgeServiceHelper::createServiceInfo))
        .ifPresent(info -> ProxyPlatformHelper.sendChannelMessageServiceSwitch(event.getPlayer().getUniqueId(), info));
    }
    // notify the management that the player successfully connected to a service
    this.management.handleFallbackConnectionSuccess(event.getPlayer());
  }

  @Subscribe
  public void handleDisconnect(@NotNull DisconnectEvent event) {
    // check if the player successfully connected to a server before
    // PRE_SERVER_JOIN will be used when the upstream server closes the connection to the player, we need to handle this
    var status = event.getLoginStatus();
    if (status == LoginStatus.SUCCESSFUL_LOGIN || status == LoginStatus.PRE_SERVER_JOIN) {
      ProxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      // update the service info
      Wrapper.getInstance().publishServiceInfoUpdate();
    }
    // always remove the player fallback profile
    this.management.removeFallbackProfile(event.getPlayer());
  }

  private @NotNull Component getReasonComponent(@NotNull KickedFromServerEvent event) {
    var playerLocale = event.getPlayer().getEffectiveLocale();
    var message = event.getServerKickReason().orElse(null);
    // use the current result if it is Notify - velocity already created a friendly reason for us
    if (message == null && event.getResult() instanceof Notify notify) {
      message = notify.getMessageComponent();
    }
    // check if we have a reason component
    if (message != null) {
      // check if we should try to translate the message
      if (message instanceof TranslatableComponent) {
        message = GlobalTranslator.render(message, playerLocale == null ? Locale.getDefault() : playerLocale);
      }
      // if the message is still not a TextComponent use the default message instead
      if (message instanceof TextComponent) {
        // wrap the message
        var baseMessage = this.management.getConfiguration().getMessage(playerLocale, "error-connecting-to-server")
          .replace("%server%", event.getServer().getServerInfo().getName())
          .replace("%reason%", LegacyComponentSerializer.legacySection().serialize(message));
        // format the message
        return serialize(baseMessage);
      }
    }
    // render the base message without a reason
    var baseMessage = this.management.getConfiguration().getMessage(playerLocale, "error-connecting-to-server")
      .replace("%server%", event.getServer().getServerInfo().getName())
      .replace("%reason%", "§cUnknown");
    // format the message
    return serialize(baseMessage);
  }
}
