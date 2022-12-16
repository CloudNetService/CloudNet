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

package eu.cloudnetservice.modules.syncproxy.platform.velocity;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import lombok.NonNull;

@Singleton
public final class VelocitySyncProxyListener {

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
    // only display a motd if there is one in the config
    if (motd != null) {
      var onlinePlayers = this.syncProxyManagement.onlinePlayerCount();
      int maxPlayers;

      if (motd.autoSlot()) {
        maxPlayers = Math.min(loginConfiguration.maxPlayers(), onlinePlayers + motd.autoSlotMaxPlayersDistance());
      } else {
        maxPlayers = loginConfiguration.maxPlayers();
      }

      var serviceInfo = this.serviceInfoHolder.serviceInfo();
      var protocolText = motd.format(serviceInfo, motd.protocolText(), onlinePlayers, maxPlayers);
      var version = event.getPing().getVersion();
      // check if a protocol text is specified in the config
      if (protocolText != null) {
        version = new ServerPing.Version(1, ComponentFormats.BUNGEE_TO_ADVENTURE.convertText(protocolText));
      }

      var builder = ServerPing.builder()
        .version(version)
        .onlinePlayers(onlinePlayers)
        .maximumPlayers(maxPlayers)
        // map the playerInfo from the config to ServerPing.SamplePlayer to display other information
        .samplePlayers(motd.playerInfo() != null ?
          Arrays.stream(motd.playerInfo())
            .filter(Objects::nonNull)
            .map(s -> new ServerPing.SamplePlayer(
              s.replace("&", "§"),
              UUID.randomUUID()
            )).toArray(ServerPing.SamplePlayer[]::new) : new ServerPing.SamplePlayer[0])
        .description(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(
          motd.format(serviceInfo, motd.firstLine() + "\n" + motd.secondLine(), onlinePlayers, maxPlayers)));

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
