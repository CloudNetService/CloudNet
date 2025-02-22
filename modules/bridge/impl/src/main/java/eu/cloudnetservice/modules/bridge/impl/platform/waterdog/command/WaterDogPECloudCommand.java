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

import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Locale;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class WaterDogPECloudCommand extends Command {

  private final ClusterNodeProvider clusterNodeProvider;
  private final PlatformBridgeManagement<?, ?> management;

  @Inject
  public WaterDogPECloudCommand(
    @NonNull ClusterNodeProvider clusterNodeProvider,
    @NonNull PlatformBridgeManagement<?, ?> management
  ) {
    super("cloudnet", CommandSettings.builder()
      .setAliases(new String[]{"cloud", "cn"})
      .setPermission("cloudnet.command.cloudnet")
      .build());
    this.clusterNodeProvider = clusterNodeProvider;
    this.management = management;
  }

  @Override
  public boolean onExecute(@NonNull CommandSender sender, @Nullable String alias, String @NonNull [] args) {
    // check if any arguments are provided
    if (args.length == 0) {
      // <prefix> /cloudnet <command>
      sender.sendMessage(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(
        this.management.configuration().prefix() + "/cloudnet <command>"));
      return true;
    }
    // get the full command line
    var commandLine = String.join(" ", args);
    // skip the permission check if the source is the console
    if (sender instanceof ProxiedPlayer) {
      // get the command info
      this.clusterNodeProvider.consoleCommandAsync(args[0]).thenAcceptAsync(info -> {
        // check if the player has the required permission
        if (info == null || !sender.hasPermission(info.permission())) {
          // no permission
          this.management.configuration().handleMessage(
            Locale.ENGLISH,
            "command-cloud-sub-command-no-permission",
            message -> message.replace("%command%", args[0]),
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
    return true;
  }

  private void executeNow(@NonNull CommandSender sender, @NonNull String commandLine) {
    for (var output : this.clusterNodeProvider.sendCommandLine(commandLine)) {
      sender.sendMessage(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(
        this.management.configuration().prefix() + output));
    }
  }
}
