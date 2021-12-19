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

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

public class CommandCloudNet extends Command {

  public CommandCloudNet() {
    super("cloudnet", CommandSettings.builder()
      .setPermission("cloudnet.command.cloudnet")
      .setUsageMessage(BridgeConfigurationProvider.load().getPrefix().replace('&', '§') + "/cloudnet <command>")
      .setAliases(new String[]{"cloud", "cl"})
      .build()
    );
  }

  @Override
  public boolean onExecute(CommandSender sender, String alias, String[] args) {
    if (args.length == 0) {
      return false;
    }

    String commandLine = String.join(" ", args);

    if (sender instanceof ProxiedPlayer) {
      CommandInfo commandInfo = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);

      if (commandInfo != null && commandInfo.getPermission() != null) {
        if (!sender.hasPermission(commandInfo.getPermission())) {
          sender
            .sendMessage(BridgeConfigurationProvider.load().getMessages().get("command-cloud-sub-command-no-permission")
              .replace("%command%", commandLine).replace('&', '§'));
          return true;
        }
      }
    }

    CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLineAsync(commandLine).onComplete(messages -> {
      for (String message : messages) {
        if (message != null) {
          sender.sendMessage((BridgeConfigurationProvider.load().getPrefix() + message).replace('&', '§'));
        }
      }
    });

    return true;
  }
}
