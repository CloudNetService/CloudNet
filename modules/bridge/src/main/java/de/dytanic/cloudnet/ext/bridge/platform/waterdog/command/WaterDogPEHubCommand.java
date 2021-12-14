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

package de.dytanic.cloudnet.ext.bridge.platform.waterdog.command;

import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.TextContainer;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

public final class WaterDogPEHubCommand extends Command {

  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;

  public WaterDogPEHubCommand(
    @NotNull PlatformBridgeManagement<ProxiedPlayer, ?> management,
    @NotNull String name,
    String @NotNull [] aliases
  ) {
    super(name, CommandSettings.builder().setAliases(aliases).build());
    this.management = management;
  }

  @Override
  public boolean onExecute(CommandSender sender, String alias, String[] args) {
    if (sender instanceof ProxiedPlayer player) {
      // check if the player is on a fallback already
      if (this.management.isOnAnyFallbackInstance(player)) {
        player.sendMessage(new TextContainer(this.management.getConfiguration().getMessage(
          Locale.ENGLISH,
          "command-hub-already-in-hub")));
      } else {
        // try to get a fallback for the player
        var hub = this.management.getFallback(player)
          .map(service -> ProxyServer.getInstance().getServerInfo(service.name()))
          .orElse(null);
        // check if a fallback was found
        if (hub != null) {
          player.connect(hub);
          player.sendMessage(new TextContainer(this.management.getConfiguration().getMessage(
            Locale.ENGLISH,
            "command-hub-success-connect"
          ).replace("%server%", hub.getServerName())));
        }
      }
    }
    return true;
  }
}
