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

package de.dytanic.cloudnet.ext.bridge.platform.bungeecord.command;

import static net.md_5.bungee.api.chat.TextComponent.fromLegacyText;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.jetbrains.annotations.NotNull;

public final class BungeeCordCloudCommand extends Command {

  private final PlatformBridgeManagement<?, ?> management;

  public BungeeCordCloudCommand(@NotNull PlatformBridgeManagement<?, ?> management) {
    super("cloudnet", "cloudnet.command.cloudnet", "cloud");
    this.management = management;
  }

  @Override
  public void execute(@NotNull CommandSender sender, String @NotNull [] args) {
    // check if any arguments are provided
    if (args.length == 0) {
      // <prefix> /cloudnet <command>
      sender.sendMessage(fromLegacyText(this.management.getConfiguration().getPrefix() + "/cloudnet <command>"));
      return;
    }
    // get the full command line
    var commandLine = String.join(" ", args);
    // skip the permission check if the source is the console
    if (sender instanceof ProxiedPlayer) {
      // get the command info
      var command = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);
      // check if the sender has the required permission to execute the command
      if (command != null) {
        if (!sender.hasPermission(command.getPermission())) {
          sender.sendMessage(fromLegacyText(this.management.getConfiguration().getMessage(
            ((ProxiedPlayer) sender).getLocale(),
            "command-cloud-sub-command-no-permission"
          ).replace("%command%", command.name())));
          return;
        }
      }
    }
    // execute the command
    CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLineAsync(commandLine).onComplete(messages -> {
      for (var line : messages) {
        sender.sendMessage(fromLegacyText(this.management.getConfiguration().getPrefix() + line));
      }
    });
  }
}
