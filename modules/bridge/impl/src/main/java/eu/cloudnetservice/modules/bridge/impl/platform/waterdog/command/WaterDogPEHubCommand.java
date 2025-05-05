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

package eu.cloudnetservice.modules.bridge.impl.platform.waterdog.command;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import java.util.Locale;
import lombok.NonNull;

public final class WaterDogPEHubCommand extends Command {

  private final ProxyServer proxyServer;
  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;

  public WaterDogPEHubCommand(
    @NonNull PlatformBridgeManagement<ProxiedPlayer, ?> management,
    @NonNull ProxyServer proxyServer,
    @NonNull String name,
    String @NonNull [] aliases
  ) {
    super(name, CommandSettings.builder().setAliases(aliases).build());
    this.management = management;
    this.proxyServer = proxyServer;
  }

  @Override
  public boolean onExecute(CommandSender sender, String alias, String[] args) {
    if (sender instanceof ProxiedPlayer player) {
      // check if the player is on a fallback already
      if (this.management.isOnAnyFallbackInstance(player)) {
        this.management.configuration().handleMessage(
          Locale.ENGLISH,
          "command-hub-already-in-hub",
          ComponentFormats.ADVENTURE_TO_BUNGEE::convertText,
          player::sendMessage);
      } else {
        // try to get a fallback for the player
        var hub = this.management.fallback(player)
          .map(service -> this.proxyServer.getServerInfo(service.name()))
          .orElse(null);
        // check if a fallback was found
        if (hub != null) {
          player.connect(hub);
          this.management.configuration().handleMessage(
            Locale.ENGLISH,
            "command-hub-success-connect",
            message -> ComponentFormats.BUNGEE_TO_ADVENTURE.convertText(
              message.replace("%server%", hub.getServerName())),
            player::sendMessage);
        }
      }
    }
    return true;
  }
}
