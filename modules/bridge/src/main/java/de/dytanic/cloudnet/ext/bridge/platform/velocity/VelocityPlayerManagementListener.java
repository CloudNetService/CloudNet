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

import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.node.event.LocalPlayerPreLoginEvent.Result;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.helper.ProxyPlatformHelper;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

final class VelocityPlayerManagementListener {

  private static final Component NO_FALLBACK = Component.translatable("velocity.error.no-available-servers");

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
    ServiceTask task = this.management.getSelfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.isMaintenance() && !event.getPlayer().hasPermission("cloudnet.bridge.maintenance")) {
        event.setResult(ComponentResult.denied(plainText().deserialize(this.management.getConfiguration().getMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-maintenance"))));
        return;
      }
      // check if a custom permission is required to join
      String permission = task.getProperties().getString("requiredPermission");
      if (permission != null && !event.getPlayer().hasPermission(permission)) {
        event.setResult(ComponentResult.denied(plainText().deserialize(this.management.getConfiguration().getMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-permission"))));
        return;
      }
    }
    // check if the player is allowed to log in
    Result loginResult = ProxyPlatformHelper.sendChannelMessagePreLogin(
      this.management.createPlayerInformation(event.getPlayer()));
    if (!loginResult.isAllowed()) {
      event.setResult(ComponentResult.denied(loginResult.getResult()));
    }
  }

  @Subscribe
  public void handleInitialServerChoose(@NotNull PlayerChooseInitialServerEvent event) {
    // filter the next fallback
    event.setInitialServer(this.management.getFallback(event.getPlayer())
      .flatMap(service -> this.proxyServer.getServer(service.getName()))
      .orElse(null));
  }

  @Subscribe
  public void handleServerKick(@NotNull KickedFromServerEvent event) {
    // check if the player is still active
    if (event.getPlayer().isActive()) {
      event.setResult(this.management.getFallback(event.getPlayer())
        .flatMap(service -> this.proxyServer.getServer(service.getName()))
        .map(KickedFromServerEvent.RedirectPlayer::create)
        .orElse(KickedFromServerEvent.DisconnectPlayer.create(NO_FALLBACK)));
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
          .getCachedService(service -> server.getServerInfo().getName().equals(service.getName()))
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
    LoginStatus status = event.getLoginStatus();
    if (status == LoginStatus.SUCCESSFUL_LOGIN || status == LoginStatus.PRE_SERVER_JOIN) {
      ProxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      // update the service info
      Wrapper.getInstance().publishServiceInfoUpdate();
    }
    // always remove the player fallback profile
    this.management.removeFallbackProfile(event.getPlayer());
  }
}
