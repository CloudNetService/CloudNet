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

package eu.cloudnetservice.modules.bridge.impl.platform.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import java.util.List;
import lombok.NonNull;

public final class VelocityHubCommand implements SimpleCommand {

  private final ProxyServer proxyServer;
  private final PlatformBridgeManagement<Player, ?> management;

  public VelocityHubCommand(@NonNull ProxyServer server, @NonNull PlatformBridgeManagement<Player, ?> management) {
    this.proxyServer = server;
    this.management = management;
  }

  @Override
  public void execute(@NonNull Invocation invocation) {
    if (invocation.source() instanceof Player player) {
      // check if the player is on a fallback already
      if (this.management.isOnAnyFallbackInstance(player)) {
        this.management.configuration().handleMessage(
          player.getEffectiveLocale(),
          "command-hub-already-in-hub",
          ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
          player::sendMessage);
      } else {
        // try to get a fallback for the player
        var hub = this.management.fallback(player)
          .flatMap(service -> this.proxyServer.getServer(service.name()))
          .orElse(null);
        // check if a fallback was found
        if (hub != null) {
          player.createConnectionRequest(hub).connectWithIndication().whenComplete((result, ex) -> {
            // check if the connection was successful
            if (result && ex == null) {
              this.management.configuration().handleMessage(
                player.getEffectiveLocale(),
                "command-hub-success-connect",
                message -> ComponentFormats.BUNGEE_TO_ADVENTURE.convert(
                  message.replace("%server%", hub.getServerInfo().getName())),
                player::sendMessage);
            } else {
              // the connection was not successful
              this.management.configuration().handleMessage(
                player.getEffectiveLocale(),
                "command-hub-no-server-found",
                ComponentFormats.BUNGEE_TO_ADVENTURE::convert,
                player::sendMessage);
            }
          });
        }
      }
    }
  }

  @Override
  public @NonNull List<String> suggest(@NonNull Invocation invocation) {
    return List.copyOf(this.management.configuration().hubCommandNames());
  }
}
