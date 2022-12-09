/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.syncproxy.platform.bungee;

import static net.md_5.bungee.api.ChatColor.translateAlternateColorCodes;

import eu.cloudnetservice.modules.bridge.platform.bungeecord.PendingConnectionProxiedPlayer;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import lombok.NonNull;
import net.md_5.bungee.api.ServerPing.PlayerInfo;
import net.md_5.bungee.api.ServerPing.Players;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeeCordSyncProxyListener implements Listener {

  private final BungeeCordSyncProxyManagement syncProxyManagement;

  public BungeeCordSyncProxyListener(@NonNull BungeeCordSyncProxyManagement syncProxyManagement) {
    this.syncProxyManagement = syncProxyManagement;
  }

  @EventHandler
  public void handleProxyPing(@NonNull ProxyPingEvent event) {
    var loginConfiguration = this.syncProxyManagement.currentLoginConfiguration();

    // check if we need to handle the proxy ping on this proxy instance
    if (loginConfiguration == null) {
      return;
    }

    var motd = this.syncProxyManagement.randomMotd();
    // only display a motd if there is one in the config
    if (motd != null) {
      var onlinePlayers = this.syncProxyManagement.onlinePlayerCount();
      int maxPlayers;

      if (motd.autoSlot()) {
        maxPlayers = Math.min(loginConfiguration.maxPlayers(), onlinePlayers + motd.autoSlotMaxPlayersDistance());
      } else {
        maxPlayers = loginConfiguration.maxPlayers();
      }

      var response = event.getResponse();

      var protocolText = motd.format(motd.protocolText(), onlinePlayers, maxPlayers);
      // check if there is a protocol text in the config
      if (protocolText != null) {
        response.setVersion(new Protocol(translateAlternateColorCodes('&', protocolText), 1));
      }

      // map the playerInfo from the config to ServerPing.Players to display other information
      var players = new Players(maxPlayers, onlinePlayers, motd.playerInfo() != null ?
        Arrays.stream(motd.playerInfo())
          .filter(Objects::nonNull)
          .map(s ->
            new PlayerInfo(translateAlternateColorCodes('&', s),
              UUID.randomUUID())).toArray(PlayerInfo[]::new) : new PlayerInfo[0]);

      response.setPlayers(players);
      response.setDescriptionComponent(new TextComponent(TextComponent.fromLegacyText(
        motd.format(translateAlternateColorCodes('&', motd.firstLine() + "\n" + motd.secondLine()),
          onlinePlayers, maxPlayers))));

      event.setResponse(response);
    }
  }

  @EventHandler
  public void handleProxyLogin(@NonNull LoginEvent event) {
    var loginConfiguration = this.syncProxyManagement.currentLoginConfiguration();
    if (loginConfiguration == null) {
      return;
    }

    var player = new PendingConnectionProxiedPlayer(proxyServer, event.getConnection());

    if (loginConfiguration.maintenance()) {
      // the player is either whitelisted or has the permission to join during maintenance, ignore him
      if (!this.syncProxyManagement.checkPlayerMaintenance(player)) {
        event.setCancelReason(this.syncProxyManagement.asComponent(
          this.syncProxyManagement.configuration().message("player-login-not-whitelisted", null)));
        event.setCancelled(true);
      }
    } else {
      // check if the proxy is full and if the player is allowed to join or not
      if (this.syncProxyManagement.onlinePlayerCount() >= loginConfiguration.maxPlayers()
        && !player.hasPermission("cloudnet.syncproxy.fulljoin")) {
        event.setCancelReason(this.syncProxyManagement.asComponent(
          this.syncProxyManagement.configuration().message("player-login-full-server", null)));
        event.setCancelled(true);
      }
    }

  }
}
