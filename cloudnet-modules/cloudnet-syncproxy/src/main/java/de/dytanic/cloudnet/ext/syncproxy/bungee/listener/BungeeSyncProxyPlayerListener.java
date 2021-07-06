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

package de.dytanic.cloudnet.ext.syncproxy.bungee.listener;

import de.dytanic.cloudnet.ext.syncproxy.bungee.BungeeSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.bungee.util.LoginProxiedPlayer;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeeSyncProxyPlayerListener implements Listener {

  private final BungeeSyncProxyManagement syncProxyManagement;

  public BungeeSyncProxyPlayerListener(BungeeSyncProxyManagement syncProxyManagement) {
    this.syncProxyManagement = syncProxyManagement;
  }

  @EventHandler
  public void handle(ProxyPingEvent event) {
    SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = this.syncProxyManagement
      .getLoginConfiguration();

    if (syncProxyProxyLoginConfiguration != null) {
      SyncProxyMotd syncProxyMotd = this.syncProxyManagement.getRandomMotd();

      if (syncProxyMotd != null) {
        String protocolText = syncProxyMotd.getProtocolText();

        int onlinePlayers = this.syncProxyManagement.getSyncProxyOnlineCount();

        int maxPlayers = syncProxyMotd.isAutoSlot() ? Math.min(
          syncProxyProxyLoginConfiguration.getMaxPlayers(),
          onlinePlayers + syncProxyMotd.getAutoSlotMaxPlayersDistance()
        ) : syncProxyProxyLoginConfiguration.getMaxPlayers();

        // explicitly checking for the second line, because Bedrock does only support one MOTD line
        String motd = ChatColor.translateAlternateColorCodes('&',
          syncProxyMotd.getFirstLine() + (syncProxyMotd.getSecondLine() == null ? ""
            : "\n" + syncProxyMotd.getSecondLine()))
          .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
          .replace("%proxy_uniqueId%", String.valueOf(Wrapper.getInstance().getServiceId().getUniqueId()))
          .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName())
          .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId())
          .replace("%online_players%", String.valueOf(onlinePlayers))
          .replace("%max_players%", String.valueOf(maxPlayers));

        ServerPing.PlayerInfo[] playerInfo = new ServerPing.PlayerInfo[syncProxyMotd.getPlayerInfo() != null
          ? syncProxyMotd.getPlayerInfo().length : 0];
        for (int i = 0; i < playerInfo.length; i++) {
          playerInfo[i] = new ServerPing.PlayerInfo(
            ChatColor.translateAlternateColorCodes('&', syncProxyMotd.getPlayerInfo()[i]),
            UUID.randomUUID().toString());
        }

        ServerPing serverPing = new ServerPing(
          new ServerPing.Protocol(ChatColor.translateAlternateColorCodes('&',
            (protocolText == null ? event.getResponse().getVersion().getName() : protocolText)
              .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
              .replace("%proxy_uniqueId%", String.valueOf(Wrapper.getInstance().getServiceId().getUniqueId()))
              .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName())
              .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId())
              .replace("%online_players%", String.valueOf(onlinePlayers))
              .replace("%max_players%", String.valueOf(maxPlayers))
          ),
            (protocolText == null ? event.getResponse().getVersion().getProtocol() : 1)),
          new ServerPing.Players(maxPlayers, onlinePlayers, playerInfo),
          new TextComponent(TextComponent.fromLegacyText(motd)),
          event.getResponse().getFaviconObject()
        );

        event.setResponse(serverPing);
      }
    }
  }

  @EventHandler
  public void handle(LoginEvent event) {
    SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = this.syncProxyManagement
      .getLoginConfiguration();

    if (syncProxyProxyLoginConfiguration != null) {
      ProxiedPlayer loginProxiedPlayer = new LoginProxiedPlayer(event.getConnection());

      if (syncProxyProxyLoginConfiguration.isMaintenance() && syncProxyProxyLoginConfiguration.getWhitelist() != null) {
        if (syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getConnection().getName())) {
          return;
        }

        UUID uniqueId = loginProxiedPlayer.getUniqueId();

        if ((uniqueId != null && syncProxyProxyLoginConfiguration.getWhitelist().contains(uniqueId.toString())) ||
          loginProxiedPlayer.hasPermission("cloudnet.syncproxy.maintenance")) {
          return;
        }

        event.setCancelled(true);
        event.setCancelReason(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
          this.syncProxyManagement.getSyncProxyConfiguration().getMessages().get("player-login-not-whitelisted"))
        ));
        return;
      }

      if (this.syncProxyManagement.getSyncProxyOnlineCount() >= syncProxyProxyLoginConfiguration.getMaxPlayers() &&
        !loginProxiedPlayer.hasPermission("cloudnet.syncproxy.fulljoin")) {
        event.setCancelled(true);
        event.setCancelReason(TextComponent.fromLegacyText(
          ChatColor.translateAlternateColorCodes('&', this.syncProxyManagement.getSyncProxyConfiguration().getMessages()
            .getOrDefault("player-login-full-server",
              "&cThe network is currently full. You need extra permissions to enter the network"))));
      }
    }
  }

  @EventHandler
  public void handleServerConnect(ServerConnectedEvent event) {
    this.syncProxyManagement.updateTabList(event.getPlayer());
  }

}
