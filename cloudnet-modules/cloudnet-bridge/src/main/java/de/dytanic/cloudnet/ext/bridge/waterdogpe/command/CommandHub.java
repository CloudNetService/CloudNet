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

package de.dytanic.cloudnet.ext.bridge.waterdogpe.command;

import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.WaterdogPECloudNetHelper;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import java.util.Arrays;

public class CommandHub extends Command {

  public CommandHub(String[] names) {
    super(names[0], CommandSettings.builder().setAliases(Arrays.copyOfRange(names, 1, names.length)).build());
  }

  @Override
  public boolean onExecute(CommandSender sender, String alias, String[] args) {
    if (!(sender instanceof ProxiedPlayer)) {
      return true;
    }

    ProxiedPlayer proxiedPlayer = (ProxiedPlayer) sender;

    if (WaterdogPECloudNetHelper.isOnMatchingFallbackInstance(proxiedPlayer)) {
      sender.sendMessage(
        BridgeConfigurationProvider.load().getMessages().get("command-hub-already-in-hub").replace('&', '§'));
      return true;
    }

    WaterdogPECloudNetHelper.connectToFallback(proxiedPlayer,
        proxiedPlayer.getServerInfo() != null ? proxiedPlayer.getServerInfo().getServerName() : null)
      .thenAccept(connectedFallback -> {
        if (connectedFallback != null) {
          sender.sendMessage(
            BridgeConfigurationProvider.load().getMessages().get("command-hub-success-connect")
              .replace("%server%", connectedFallback.getName())
              .replace('&', '§')
          );
        } else {
          sender.sendMessage(
            BridgeConfigurationProvider.load().getMessages().get("command-hub-no-server-found").replace('&', '§'));
        }
      });

    return true;
  }

}
