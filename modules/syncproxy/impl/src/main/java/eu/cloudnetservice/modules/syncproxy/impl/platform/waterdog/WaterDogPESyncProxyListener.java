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

package eu.cloudnetservice.modules.syncproxy.impl.platform.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class WaterDogPESyncProxyListener {

  private final ServiceInfoHolder serviceInfoHolder;
  private final WaterDogPESyncProxyManagement syncProxyManagement;

  @Inject
  public WaterDogPESyncProxyListener(
    @NonNull WaterDogPESyncProxyManagement syncProxyManagement,
    @NonNull ProxyServer proxyServer,
    @NonNull ServiceInfoHolder serviceInfoHolder
  ) {
    this.syncProxyManagement = syncProxyManagement;
    this.serviceInfoHolder = serviceInfoHolder;

    // subscribe to the events and redirect them to the methods to handle them
    proxyServer.getEventManager().subscribe(ProxyPingEvent.class, this::handleProxyPing);
    proxyServer.getEventManager().subscribe(PlayerLoginEvent.class, this::handleProxyLogin);
  }

  private void handleProxyPing(@NonNull ProxyPingEvent event) {
    var loginConfiguration = this.syncProxyManagement.currentLoginConfiguration();
    // check if we need to handle the proxy ping on this proxy instance
    if (loginConfiguration == null) {
      return;
    }

    var motd = this.syncProxyManagement.randomMotd();
    // only display a motd if there is one in the config
    if (motd != null) {
      var onlinePlayers = this.syncProxyManagement.onlinePlayerCount();
      event.setPlayerCount(onlinePlayers);

      int maxPlayers;
      if (motd.autoSlot()) {
        maxPlayers = Math.min(loginConfiguration.maxPlayers(), onlinePlayers + motd.autoSlotMaxPlayersDistance());
      } else {
        maxPlayers = loginConfiguration.maxPlayers();
      }
      event.setMaximumPlayerCount(maxPlayers);

      // bedrock has just to lines that are separated from each other
      var serviceInfo = this.serviceInfoHolder.serviceInfo();
      var mainMotd = SyncProxyConfiguration.fillCommonPlaceholders(
        serviceInfo,
        motd.firstLine(),
        onlinePlayers,
        maxPlayers);
      var subMotd = SyncProxyConfiguration.fillCommonPlaceholders(
        serviceInfo,
        motd.secondLine(),
        onlinePlayers,
        maxPlayers);

      event.setMotd(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(mainMotd));
      event.setSubMotd(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(subMotd));
    }
  }

  private void handleProxyLogin(@NonNull PlayerLoginEvent event) {
    var loginConfiguration = this.syncProxyManagement.currentLoginConfiguration();
    if (loginConfiguration == null) {
      return;
    }

    var proxiedPlayer = event.getPlayer();
    if (loginConfiguration.maintenance()) {
      // the player is either whitelisted or has the permission to join during maintenance, ignore him
      if (!this.syncProxyManagement.checkPlayerMaintenance(proxiedPlayer)) {
        var cancelReason = this.syncProxyManagement.configuration().message("player-login-not-whitelisted", null);
        event.setCancelReason(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(cancelReason));
        event.setCancelled(true);
      }
    } else {
      // check if the proxy is full and if the player is allowed to join or not
      if (this.syncProxyManagement.onlinePlayerCount() >= loginConfiguration.maxPlayers()
        && !proxiedPlayer.hasPermission("cloudnet.syncproxy.fulljoin")) {
        var cancelReason = this.syncProxyManagement.configuration().message("player-login-full-server", null);
        event.setCancelReason(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(cancelReason));
        event.setCancelled(true);
      }
    }
  }
}
