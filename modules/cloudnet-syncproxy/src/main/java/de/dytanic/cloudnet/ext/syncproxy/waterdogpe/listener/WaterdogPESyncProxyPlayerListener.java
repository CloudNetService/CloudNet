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

package de.dytanic.cloudnet.ext.syncproxy.waterdogpe.listener;

import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.waterdogpe.WaterdogPESyncProxyManagement;
import de.dytanic.cloudnet.wrapper.Wrapper;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;

public class WaterdogPESyncProxyPlayerListener {

  public WaterdogPESyncProxyPlayerListener(WaterdogPESyncProxyManagement management) {
    EventManager eventManager = ProxyServer.getInstance().getEventManager();

    eventManager.subscribe(PlayerLoginEvent.class, event -> {
      SyncProxyProxyLoginConfiguration configuration = management.getLoginConfiguration();
      if (configuration != null) {
        if (configuration.isMaintenance()
          && !management.isWhitelisted(event.getPlayer(), configuration.getWhitelist())) {
          event.setCancelled(true);
          event.setCancelReason(
            management.getSyncProxyConfiguration().getMessages().get("player-login-not-whitelisted").replace('&', '§'));
        } else if (management.getSyncProxyOnlineCount() >= configuration.getMaxPlayers()
          && !event.getPlayer().hasPermission("cloudnet.syncproxy.fulljoin")) {
          event.setCancelled(true);
          event.setCancelReason(management.getSyncProxyConfiguration().getMessages().getOrDefault(
            "player-login-full-server",
            "&cThe network is currently full. You need extra permissions to enter the network"
          ).replace('&', '§'));
        }
      }
    });

    eventManager.subscribe(ProxyPingEvent.class, event -> {
      SyncProxyProxyLoginConfiguration configuration = management.getLoginConfiguration();
      if (configuration != null) {
        SyncProxyMotd proxyMotd = management.getRandomMotd();
        if (proxyMotd != null) {
          // auto slot
          int onlinePlayers = management.getSyncProxyOnlineCount();
          int maxPlayers = proxyMotd.isAutoSlot() ? Math.min(
            configuration.getMaxPlayers(),
            onlinePlayers + proxyMotd.getAutoSlotMaxPlayersDistance()
          ) : configuration.getMaxPlayers();
          // motd
          String motd = this.replaceMotdLine(proxyMotd.getFirstLine(), onlinePlayers, maxPlayers).replace('&', '§');
          String subMotd = this.replaceMotdLine(proxyMotd.getSecondLine(), onlinePlayers, maxPlayers).replace('&', '§');

          event.setPlayerCount(onlinePlayers);
          event.setMaximumPlayerCount(maxPlayers);

          event.setMotd(motd);
          event.setSubMotd(subMotd);
        }
      }
    });
  }

  private String replaceMotdLine(String line, int onlinePlayers, int maxPlayers) {
    return line == null ? "" : line
      .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
      .replace("%proxy_uniqueId%", String.valueOf(Wrapper.getInstance().getServiceId().getUniqueId()))
      .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName())
      .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .replace("%online_players%", String.valueOf(onlinePlayers))
      .replace("%max_players%", String.valueOf(maxPlayers));
  }
}
