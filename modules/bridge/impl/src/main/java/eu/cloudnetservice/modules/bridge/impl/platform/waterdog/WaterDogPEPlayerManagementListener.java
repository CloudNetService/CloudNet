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

package eu.cloudnetservice.modules.bridge.impl.platform.waterdog;

import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.defaults.InitialServerConnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.TransferCompleteEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.scheduler.WaterdogScheduler;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.helper.ProxyPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Locale;
import lombok.NonNull;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@Singleton
public final class WaterDogPEPlayerManagementListener {

  private final WaterdogScheduler scheduler;
  private final ServiceInfoHolder serviceInfoHolder;
  private final ProxyPlatformHelper proxyPlatformHelper;
  private final PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management;

  @Inject
  public WaterDogPEPlayerManagementListener(
    @NonNull EventManager eventManager,
    @NonNull WaterdogScheduler scheduler,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull ProxyPlatformHelper proxyPlatformHelper,
    @NonNull PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management
  ) {
    this.scheduler = scheduler;
    this.serviceInfoHolder = serviceInfoHolder;
    this.proxyPlatformHelper = proxyPlatformHelper;
    this.management = management;
    // subscribe to all events
    eventManager.subscribe(PlayerLoginEvent.class, this::handleLogin);
    eventManager.subscribe(TransferCompleteEvent.class, this::handleTransfer);
    eventManager.subscribe(PlayerDisconnectEvent.class, this::handleDisconnected);
    eventManager.subscribe(InitialServerConnectedEvent.class, this::handleInitialConnect);
  }

  private void handleLogin(@NonNull PlayerLoginEvent event) {
    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.maintenance() && !event.getPlayer().hasPermission("cloudnet.bridge.maintenance")) {
        event.setCancelled(true);
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-maintenance",
          event::setCancelReason);
        return;
      }
      // check if a custom permission is required to join
      var permission = task.propertyHolder().getString("requiredPermission");
      if (permission != null && !event.getPlayer().hasPermission(permission)) {
        event.setCancelled(true);
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "proxy-join-cancel-because-permission",
          event::setCancelReason);
        return;
      }
    }
    // check if the player is allowed to log in
    var loginResult = this.proxyPlatformHelper.sendChannelMessagePreLogin(
      this.management.createPlayerInformation(event.getPlayer()));
    if (!loginResult.permitLogin()) {
      event.setCancelled(true);
      event.setCancelReason(LegacyComponentSerializer.legacySection().serialize(loginResult.result()));
    }
  }

  private void handleInitialConnect(@NonNull InitialServerConnectedEvent event) {
    // the player logged in successfully if he is now connected to a service for the first time
    this.proxyPlatformHelper.sendChannelMessageLoginSuccess(
      this.management.createPlayerInformation(event.getPlayer()),
      this.management
        .cachedService(service -> service.name().equals(event.getInitialDownstream().getServerInfo().getServerName()))
        .map(NetworkServiceInfo::fromServiceInfoSnapshot)
        .orElse(null));
    // update the service info
    this.serviceInfoHolder.publishServiceInfoUpdate();
    // notify the management that the player successfully connected to a service
    this.management.handleFallbackConnectionSuccess(event.getPlayer());
  }

  private void handleTransfer(@NonNull TransferCompleteEvent event) {
    this.management
      .cachedService(service -> service.name().equals(event.getNewClient().getServerInfo().getServerName()))
      .map(NetworkServiceInfo::fromServiceInfoSnapshot)
      .ifPresent(serviceInfo -> {
        // the player switched the service
        this.proxyPlatformHelper.sendChannelMessageServiceSwitch(event.getPlayer().getUniqueId(), serviceInfo);
      });
    // notify the management that the player successfully connected to a service
    this.management.handleFallbackConnectionSuccess(event.getPlayer());
  }

  private void handleDisconnected(@NonNull PlayerDisconnectEvent event) {
    // check if the player successfully connected to a server before
    if (event.getPlayer().getServerInfo() != null) {
      this.proxyPlatformHelper.sendChannelMessageDisconnected(event.getPlayer().getUniqueId());
      // update the service info
      this.scheduler.scheduleDelayed(this.serviceInfoHolder::publishServiceInfoUpdate, 1);
    }
    // always remove the player fallback profile
    this.management.removeFallbackProfile(event.getPlayer());
  }
}
