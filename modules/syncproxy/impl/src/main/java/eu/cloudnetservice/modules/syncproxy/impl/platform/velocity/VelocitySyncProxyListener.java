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

package eu.cloudnetservice.modules.syncproxy.impl.platform.velocity;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import lombok.NonNull;

@Singleton
public final class VelocitySyncProxyListener {

  private static final ServerPing.SamplePlayer[] EMPTY_SAMPLE_PLAYER = new ServerPing.SamplePlayer[0];

  private final ServiceInfoHolder serviceInfoHolder;
  private final VelocitySyncProxyManagement syncProxyManagement;

  @Inject
  public VelocitySyncProxyListener(
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull VelocitySyncProxyManagement syncProxyManagement
  ) {
    this.serviceInfoHolder = serviceInfoHolder;
    this.syncProxyManagement = syncProxyManagement;
  }

  @Subscribe
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

      var serviceInfo = this.serviceInfoHolder.serviceInfo();
      var protocolText = SyncProxyConfiguration.fillCommonPlaceholders(
        serviceInfo,
        motd.protocolText(),
        onlinePlayers,
        maxPlayers);
      var version = event.getPing().getVersion();
      // check if a protocol text is specified in the config
      if (protocolText != null) {
        version = new ServerPing.Version(1, ComponentFormats.BUNGEE_TO_ADVENTURE.convertText(protocolText));
      }

      // convert the player info into individual player samples
      var samplePlayers = EMPTY_SAMPLE_PLAYER;
      if (motd.playerInfo() != null) {
        // convert the player info into individual player samples
        samplePlayers = Arrays.stream(motd.playerInfo())
          .filter(Objects::nonNull)
          .map(info -> SyncProxyConfiguration.fillCommonPlaceholders(serviceInfo, info, onlinePlayers, maxPlayers))
          .map(ComponentFormats.ADVENTURE_TO_BUNGEE::convertText)
          .map(info -> new ServerPing.SamplePlayer(info, UUID.randomUUID()))
          .toArray(ServerPing.SamplePlayer[]::new);
      }

      // construct the description for the response
      var description = SyncProxyConfiguration.fillCommonPlaceholders(
        serviceInfo,
        motd.firstLine() + "\n" + motd.secondLine(),
        onlinePlayers,
        maxPlayers);

      // construct the response to the ping event
      var builder = ServerPing.builder()
        .version(version)
        .onlinePlayers(onlinePlayers)
        .maximumPlayers(maxPlayers)
        .samplePlayers(samplePlayers)
        .description(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(description));

      event.getPing().getFavicon().ifPresent(builder::favicon);
      event.getPing().getModinfo().ifPresent(builder::mods);

      event.setPing(builder.build());
    }
  }

  @Subscribe
  public void handlePlayerLogin(@NonNull LoginEvent event) {
    var loginConfiguration = this.syncProxyManagement.currentLoginConfiguration();
    if (loginConfiguration != null) {
      var player = event.getPlayer();
      if (loginConfiguration.maintenance()) {
        // the player is either whitelisted or has the permission to join during maintenance, ignore him
        if (!this.syncProxyManagement.checkPlayerMaintenance(player)) {
          var reason = ComponentFormats.BUNGEE_TO_ADVENTURE.convert(
            this.syncProxyManagement.configuration().message("player-login-not-whitelisted", null));
          event.setResult(ResultedEvent.ComponentResult.denied(reason));
        }
      } else {
        // check if the proxy is full and if the player is allowed to join or not
        if (this.syncProxyManagement.onlinePlayerCount() >= loginConfiguration.maxPlayers()
          && !player.hasPermission("cloudnet.syncproxy.fulljoin")) {
          var reason = ComponentFormats.BUNGEE_TO_ADVENTURE.convert(
            this.syncProxyManagement.configuration().message("player-login-full-server", null));
          event.setResult(ResultedEvent.ComponentResult.denied(reason));
        }
      }
    }
  }
}
