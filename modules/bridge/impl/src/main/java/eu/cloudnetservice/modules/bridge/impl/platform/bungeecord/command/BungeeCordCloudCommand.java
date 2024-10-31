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

import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

@Singleton
public final class BungeeCordCloudCommand extends Command implements TabExecutor {

  private final ClusterNodeProvider clusterNodeProvider;
  private final PlatformBridgeManagement<?, ?> management;

  @Inject
  public BungeeCordCloudCommand(
    @NonNull ClusterNodeProvider clusterNodeProvider,
    @NonNull PlatformBridgeManagement<?, ?> management
  ) {
    super("cloudnet", "cloudnet.command.cloudnet", "cloud");
    this.clusterNodeProvider = clusterNodeProvider;
    this.management = management;
  }

  @Override
  public void execute(@NonNull CommandSender sender, @NonNull String[] args) {
    // check if any arguments are provided
    if (args.length == 0) {
      // <prefix> /cloudnet <command>
      sender.sendMessage(
        ComponentFormats.ADVENTURE_TO_BUNGEE.convert(this.management.configuration().prefix() + "/cloudnet <command>"));
      return;
    }
    // get the full command line
    var commandLine = String.join(" ", args);
    // skip the permission check if the source is the console
    if (sender instanceof ProxiedPlayer player) {
      // get the command info
      this.clusterNodeProvider.consoleCommandAsync(args[0]).thenAcceptAsync(info -> {
        // check if the player has the required permission
        if (info == null || !sender.hasPermission(info.permission())) {
          this.management.configuration().handleMessage(
            player.getLocale(),
            "command-cloud-sub-command-no-permission",
            message -> ComponentFormats.ADVENTURE_TO_BUNGEE.convert(message.replace("%command%", args[0])),
            sender::sendMessage);
        } else {
          // execute command
          this.executeNow(sender, commandLine);
        }
      });
    } else {
      // just execute
      this.executeNow(sender, commandLine);
    }
  }

  private void executeNow(@NonNull CommandSender sender, @NonNull String commandLine) {
    for (var output : this.clusterNodeProvider.sendCommandLine(commandLine)) {
      sender.sendMessage(ComponentFormats.ADVENTURE_TO_BUNGEE.convert(this.management.configuration().prefix() + output));
    }
  }

  @Override
  public @NonNull Iterable<String> onTabComplete(@NonNull CommandSender sender, @NonNull String[] args) {
    return this.clusterNodeProvider.consoleTabCompleteResults(String.join(" ", args));
  }
}
