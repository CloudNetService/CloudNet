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

package eu.cloudnetservice.modules.syncproxy.platform.bungee;

import eu.cloudnetservice.modules.bridge.platform.bungeecord.BungeeCordHelper;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import lombok.NonNull;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ServerPing.PlayerInfo;
import net.md_5.bungee.api.ServerPing.Players;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

@Singleton
public final class BungeeCordSyncProxyListener implements Listener {

  private static final PlayerInfo[] EMPTY_PLAYER_INFO = new PlayerInfo[0];

  private final BungeeCordHelper bungeeCordHelper;
  private final ServiceInfoHolder serviceInfoHolder;
  private final BungeeCordSyncProxyManagement syncProxyManagement;

  @Inject
  public BungeeCordSyncProxyListener(
    @NonNull BungeeCordHelper bungeeCordHelper,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull BungeeCordSyncProxyManagement syncProxyManagement
  ) {
    this.bungeeCordHelper = bungeeCordHelper;
    this.serviceInfoHolder = serviceInfoHolder;
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
    // only display the motd if its desired
    if (motd != null) {
      var onlinePlayers = this.syncProxyManagement.onlinePlayerCount();
      int maxPlayers;
      // calculate the slots to display based on the autoslot configuration
      if (motd.autoSlot()) {
        maxPlayers = Math.min(loginConfiguration.maxPlayers(), onlinePlayers + motd.autoSlotMaxPlayersDistance());
      } else {
        maxPlayers = loginConfiguration.maxPlayers();
      }

      var response = event.getResponse();

      var serviceInfo = this.serviceInfoHolder.serviceInfo();
      var protocolText = motd.protocolText();
      // check if there is a protocol text in the config
      if (protocolText != null) {
        var placeholders = new HashMap<String, String>();
        SyncProxyConfiguration.fillCommonPlaceholders(placeholders, serviceInfo, onlinePlayers, maxPlayers);
        response.setVersion(new Protocol(
          LegacyComponentSerializer.legacySection().serialize(
            MiniMessage.miniMessage().deserialize(
              protocolText,
              SyncProxyConfiguration.adventurePlaceholders(serviceInfo, onlinePlayers, maxPlayers)
            )
          ),
          1));
      }

      var playerSamples = EMPTY_PLAYER_INFO;
      if (motd.playerInfo() != null) {
        // convert the player info into individual player samples
        playerSamples = Arrays.stream(motd.playerInfo())
          .filter(Objects::nonNull)
          .map(info -> MiniMessage.miniMessage().deserialize(
            info,
            SyncProxyConfiguration.adventurePlaceholders(serviceInfo, onlinePlayers, maxPlayers)
          ))
          .map(LegacyComponentSerializer.legacySection()::serialize)
          .map(info -> new PlayerInfo(info, UUID.randomUUID()))
          .toArray(PlayerInfo[]::new);
      }

      var players = new Players(maxPlayers, onlinePlayers, playerSamples);
      response.setPlayers(players);

      var description = MiniMessage.miniMessage().deserialize(
          motd.firstLine(),
          SyncProxyConfiguration.adventurePlaceholders(
            serviceInfo,
            onlinePlayers,
            maxPlayers
          )
      )
        .appendNewline()
        .append(MiniMessage.miniMessage().deserialize(
          motd.secondLine(),
          SyncProxyConfiguration.adventurePlaceholders(
            serviceInfo,
            onlinePlayers,
            maxPlayers
          )
      ));

      // thanks bungeecord - convert the component array into a single component
      response.setDescriptionComponent(new TextComponent(BungeeComponentSerializer.get().serialize(description)));

      event.setResponse(response);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handleProxyLogin(@NonNull ServerConnectEvent event) {
    // only handle the initial proxy connect and the event was not cancelled by someone else
    if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY || event.isCancelled()) {
      return;
    }

    var loginConfiguration = this.syncProxyManagement.currentLoginConfiguration();
    if (loginConfiguration == null) {
      return;
    }

    var player = event.getPlayer();
    if (loginConfiguration.maintenance()) {
      // the player is either whitelisted or has the permission to join during maintenance, ignore him
      if (!this.syncProxyManagement.checkPlayerMaintenance(player)) {
        player.disconnect(BungeeComponentSerializer.get()
          .serialize(this.syncProxyManagement.configuration().message("player-login-not-whitelisted")));
        event.setCancelled(true);
      }
    } else {
      // check if the proxy is full and if the player is allowed to join or not
      if (this.syncProxyManagement.onlinePlayerCount() >= loginConfiguration.maxPlayers()
        && !player.hasPermission("cloudnet.syncproxy.fulljoin")) {
        player.disconnect(BungeeComponentSerializer.get()
          .serialize(this.syncProxyManagement.configuration().message("player-login-full-server")));
        event.setCancelled(true);
      }
    }
  }
}
