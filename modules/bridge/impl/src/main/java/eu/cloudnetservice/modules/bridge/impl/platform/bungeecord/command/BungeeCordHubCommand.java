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

package eu.cloudnetservice.modules.bridge.impl.platform.bungeecord.command;

import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.Command;

public final class BungeeCordHubCommand extends Command {

  private final ProxyServer proxyServer;
  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;

  public BungeeCordHubCommand(
    @NonNull ProxyServer proxyServer,
    @NonNull PlatformBridgeManagement<ProxiedPlayer, ?> management,
    @NonNull String name,
    String @NonNull ... aliases
  ) {
    super(name, null, aliases);
    this.management = management;
    this.proxyServer = proxyServer;
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (sender instanceof ProxiedPlayer player) {
      // check if the player is on a fallback already
      if (this.management.isOnAnyFallbackInstance(player)) {
        this.management.configuration().handleMessage(
          player.getLocale(),
          "command-hub-already-in-hub",
          ComponentFormats.ADVENTURE_TO_BUNGEE::convert,
          player::sendMessage);
      } else {
        // try to get a fallback for the player
        var hub = this.management.fallback(player)
          .map(service -> this.proxyServer.getServerInfo(service.name()))
          .orElse(null);
        // check if a fallback was found
        if (hub != null) {
          player.connect(hub, (result, ex) -> {
            // check if the connection was successful
            if (result && ex == null) {
              this.management.configuration().handleMessage(
                player.getLocale(),
                "command-hub-success-connect",
                message -> ComponentFormats.ADVENTURE_TO_BUNGEE.convert(message.replace("%server%", hub.getName())),
                player::sendMessage);
            } else {
              // the connection was not successful
              this.management.configuration().handleMessage(
                player.getLocale(),
                "command-hub-no-server-found",
                ComponentFormats.ADVENTURE_TO_BUNGEE::convert,
                player::sendMessage);
            }
          }, Reason.COMMAND);
        }
      }
    }
  }
}
