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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyLoginConfiguration;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyMotd;
import org.jetbrains.annotations.NotNull;

public final class WaterDogPESyncProxyListener {

  private final WaterDogPESyncProxyManagement syncProxyManagement;

  public WaterDogPESyncProxyListener(
    @NotNull WaterDogPESyncProxyManagement syncProxyManagement,
    @NotNull ProxyServer proxyServer
  ) {
    this.syncProxyManagement = syncProxyManagement;

    proxyServer.getEventManager().subscribe(ProxyPingEvent.class, this::handleProxyPing);
  }

  public void handleProxyPing(@NotNull ProxyPingEvent event) {
    SyncProxyLoginConfiguration loginConfiguration = this.syncProxyManagement.getCurrentLoginConfiguration();

    SyncProxyMotd motd;
    if (loginConfiguration == null || (motd = this.syncProxyManagement.getRandomMotd()) == null) {
      return;
    }

    int onlinePlayers = this.syncProxyManagement.getOnlinePlayerCount();
    int maxPlayers;

    event.setPlayerCount(onlinePlayers);

    if (motd.isAutoSlot()) {
      maxPlayers = Math.min(loginConfiguration.getMaxPlayers(), onlinePlayers + motd.getAutoSlotMaxPlayersDistance());
    } else {
      maxPlayers = loginConfiguration.getMaxPlayers();
    }
    event.setMaximumPlayerCount(maxPlayers);

    String mainMotd = motd.format(motd.getFirstLine(), onlinePlayers, maxPlayers);
    String subMotd = motd.format(motd.getSecondLine(), onlinePlayers, maxPlayers);

    event.setMotd(mainMotd);
    event.setSubMotd(subMotd);
  }

  public void handleProxyLogin(@NotNull PlayerLoginEvent event) {
    SyncProxyLoginConfiguration loginConfiguration = this.syncProxyManagement.getCurrentLoginConfiguration();
    if (loginConfiguration == null) {
      return;
    }

    ProxiedPlayer proxiedPlayer = event.getPlayer();

    if (loginConfiguration.isMaintenance()) {
      // the player is either whitelisted or has the permission to join during maintenance, ignore him
      if (this.syncProxyManagement.checkPlayerMaintenance(proxiedPlayer)) {
        return;
      }
      event.setCancelReason(
        this.syncProxyManagement.getConfiguration().getMessage("player-login-not-whitelisted", null));
      event.setCancelled(true);

      return;
    }

    if (this.syncProxyManagement.getOnlinePlayerCount() >= loginConfiguration.getMaxPlayers()
      && !proxiedPlayer.hasPermission("cloudnet.syncproxy.fulljoin")) {
      event.setCancelReason(this.syncProxyManagement.getConfiguration().getMessage("player-login-full-server", null));
      event.setCancelled(true);
    }
  }
}
