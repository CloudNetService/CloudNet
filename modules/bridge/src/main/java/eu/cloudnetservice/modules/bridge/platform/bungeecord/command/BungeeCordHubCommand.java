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

package eu.cloudnetservice.modules.bridge.platform.bungeecord.command;

import static eu.cloudnetservice.modules.bridge.platform.bungeecord.BungeeCordHelper.translateToComponent;

import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.Command;

public final class BungeeCordHubCommand extends Command {

  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;

  public BungeeCordHubCommand(
    @NonNull PlatformBridgeManagement<ProxiedPlayer, ?> management,
    @NonNull String name,
    String @NonNull ... aliases
  ) {
    super(name, null, aliases);
    this.management = management;
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (sender instanceof ProxiedPlayer player) {
      // check if the player is on a fallback already
      if (this.management.isOnAnyFallbackInstance(player)) {
        player.sendMessage(translateToComponent(this.management.configuration().message(
          player.getLocale(),
          "command-hub-already-in-hub")));
      } else {
        // try to get a fallback for the player
        ServerInfo hub = this.management.fallback(player)
          .map(service -> ProxyServer.getInstance().getServerInfo(service.name()))
          .orElse(null);
        // check if a fallback was found
        if (hub != null) {
          player.connect(hub, (result, ex) -> {
            // check if the connection was successful
            if (result && ex == null) {
              player.sendMessage(translateToComponent(this.management.configuration().message(
                player.getLocale(),
                "command-hub-success-connect"
              ).replace("%server%", hub.getName())));
            } else {
              // the connection was not successful
              player.sendMessage(translateToComponent(this.management.configuration().message(
                player.getLocale(),
                "command-hub-no-server-found")));
            }
          }, Reason.COMMAND);
        }
      }
    }
  }
}
