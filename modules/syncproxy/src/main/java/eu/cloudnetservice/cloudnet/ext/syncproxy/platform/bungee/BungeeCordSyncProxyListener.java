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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.bungee;

import de.dytanic.cloudnet.ext.bridge.platform.bungeecord.PendingConnectionProxiedPlayer;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyLoginConfiguration;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyMotd;
import java.util.Arrays;
import java.util.UUID;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.ServerPing.PlayerInfo;
import net.md_5.bungee.api.ServerPing.Players;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

public final class BungeeCordSyncProxyListener implements Listener {

  private final BungeeCordSyncProxyManagement syncProxyManagement;

  public BungeeCordSyncProxyListener(@NotNull BungeeCordSyncProxyManagement syncProxyManagement) {
    this.syncProxyManagement = syncProxyManagement;
  }

  @EventHandler
  public void handleProxyPing(@NotNull ProxyPingEvent event) {
    SyncProxyLoginConfiguration loginConfiguration = this.syncProxyManagement.getCurrentLoginConfiguration();

    // check if we need to handle the proxy ping on this proxy instance
    if (loginConfiguration == null) {
      return;
    }

    SyncProxyMotd motd = this.syncProxyManagement.getRandomMotd();
    // only display a motd if there is one in the config
    if (motd != null) {
      int onlinePlayers = this.syncProxyManagement.getOnlinePlayerCount();
      int maxPlayers;

      if (motd.isAutoSlot()) {
        maxPlayers = Math.min(loginConfiguration.getMaxPlayers(), onlinePlayers + motd.getAutoSlotMaxPlayersDistance());
      } else {
        maxPlayers = loginConfiguration.getMaxPlayers();
      }

      ServerPing response = event.getResponse();

      String protocolText = motd.format(motd.getProtocolText(), onlinePlayers, maxPlayers);
      // check if there is a protocol text in the config
      if (protocolText != null) {
        response.setVersion(new Protocol(protocolText, 1));
      }

      // map the playerInfo from the config to ServerPing.Players to display other information
      ServerPing.Players players = new Players(maxPlayers, onlinePlayers,
        Arrays.stream(motd.getPlayerInfo()).map(s -> new PlayerInfo(s.replace("&", "§"),
          UUID.randomUUID())).toArray(PlayerInfo[]::new));

      response.setPlayers(players);
      response.setDescriptionComponent(new TextComponent(TextComponent.fromLegacyText(
        motd.format(motd.getFirstLine() + "\n" + motd.getSecondLine(), onlinePlayers, maxPlayers))));

      event.setResponse(response);
    }
  }

  @EventHandler
  public void handleProxyLogin(@NotNull LoginEvent event) {
    SyncProxyLoginConfiguration loginConfiguration = this.syncProxyManagement.getCurrentLoginConfiguration();
    if (loginConfiguration == null) {
      return;
    }

    ProxiedPlayer player = new PendingConnectionProxiedPlayer(event.getConnection());

    if (loginConfiguration.isMaintenance()) {
      // the player is either whitelisted or has the permission to join during maintenance, ignore him
      if (this.syncProxyManagement.checkPlayerMaintenance(player)) {
        return;
      }
      event.setCancelReason(TextComponent.fromLegacyText(
        this.syncProxyManagement.getConfiguration().getMessage("player-login-not-whitelisted", null)));
      event.setCancelled(true);

      return;
    }
    // check if the proxy is full and if the player is allowed to join or not
    if (this.syncProxyManagement.getOnlinePlayerCount() >= loginConfiguration.getMaxPlayers()
      && !player.hasPermission("cloudnet.syncproxy.fulljoin")) {
      event.setCancelReason(TextComponent.fromLegacyText(
        this.syncProxyManagement.getConfiguration().getMessage("player-login-full-server", null)));
      event.setCancelled(true);
    }
  }
}
