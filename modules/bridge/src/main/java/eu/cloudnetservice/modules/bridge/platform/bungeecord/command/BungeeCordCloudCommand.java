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

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public final class BungeeCordCloudCommand extends Command implements TabExecutor {

  private final PlatformBridgeManagement<?, ?> management;

  public BungeeCordCloudCommand(@NonNull PlatformBridgeManagement<?, ?> management) {
    super("cloudnet", "cloudnet.command.cloudnet", "cloud");
    this.management = management;
  }

  @Override
  public void execute(@NonNull CommandSender sender, String @NonNull [] args) {
    // check if any arguments are provided
    if (args.length == 0) {
      // <prefix> /cloudnet <command>
      sender.sendMessage(translateToComponent(this.management.configuration().prefix() + "/cloudnet <command>"));
      return;
    }
    // get the full command line
    var commandLine = String.join(" ", args);
    // skip the permission check if the source is the console
    if (sender instanceof ProxiedPlayer) {
      // get the command info
      var command = CloudNetDriver.instance().clusterNodeProvider().consoleCommand(commandLine);
      // check if the sender has the required permission to execute the command
      if (command != null) {
        if (!sender.hasPermission(command.permission())) {
          sender.sendMessage(translateToComponent(this.management.configuration().message(
            ((ProxiedPlayer) sender).getLocale(),
            "command-cloud-sub-command-no-permission"
          ).replace("%command%", command.name())));
          return;
        }
      }
    }
    // execute the command
    CloudNetDriver.instance().clusterNodeProvider().sendCommandLineAsync(commandLine).thenAccept(messages -> {
      for (var line : messages) {
        sender.sendMessage(translateToComponent(this.management.configuration().prefix() + line));
      }
    });
  }

  @Override
  public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
    return CloudNetDriver.instance().clusterNodeProvider().consoleTabCompleteResults(String.join(" ", args));
  }
}
