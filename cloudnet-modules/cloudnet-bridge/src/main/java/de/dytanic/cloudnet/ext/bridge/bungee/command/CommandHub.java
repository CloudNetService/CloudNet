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

package de.dytanic.cloudnet.ext.bridge.bungee.command;

import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import java.util.Arrays;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public final class CommandHub extends Command {

  private final String[] aliases;

  public CommandHub(String[] names) {
    super(names[0]);
    this.aliases = Arrays.copyOfRange(names, 1, names.length);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (!(sender instanceof ProxiedPlayer)) {
      return;
    }

    ProxiedPlayer proxiedPlayer = (ProxiedPlayer) sender;

    if (BungeeCloudNetHelper.isOnMatchingFallbackInstance(proxiedPlayer)) {
      sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
        BridgeConfigurationProvider.load().getMessages().get("command-hub-already-in-hub"))));
      return;
    }

    BungeeCloudNetHelper.connectToFallback(proxiedPlayer,
      proxiedPlayer.getServer() != null ? proxiedPlayer.getServer().getInfo().getName() : null)
      .thenAccept(connectedFallback -> {
        if (connectedFallback != null) {
          sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
            BridgeConfigurationProvider.load().getMessages().get("command-hub-success-connect"))
            .replace("%server%", connectedFallback.getName())
          ));
        } else {
          sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
            BridgeConfigurationProvider.load().getMessages().get("command-hub-no-server-found"))));
        }
      });
  }

  @Override
  public String[] getAliases() {
    return this.aliases;
  }

}
