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

package eu.cloudnetservice.modules.bridge.platform.waterdog.command;

import static eu.cloudnetservice.ext.adventure.AdventureSerializerUtil.serializeToString;

import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import java.util.Locale;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class WaterDogPECloudCommand extends Command {

  private final PlatformBridgeManagement<?, ?> management;

  public WaterDogPECloudCommand(@NonNull PlatformBridgeManagement<?, ?> management) {
    super("cloudnet", CommandSettings.builder()
      .setAliases(new String[]{"cloud", "cn"})
      .setPermission("cloudnet.command.cloudnet")
      .build());
    this.management = management;
  }

  @Override
  public boolean onExecute(@NonNull CommandSender sender, @Nullable String alias, String @NonNull [] args) {
    // check if any arguments are provided
    if (args.length == 0) {
      // <prefix> /cloudnet <command>
      sender.sendMessage(serializeToString(this.management.configuration().prefix() + "/cloudnet <command>"));
      return true;
    }
    // get the full command line
    var commandLine = String.join(" ", args);
    // skip the permission check if the source is the console
    if (sender instanceof ProxiedPlayer) {
      // get the command info
      CloudNetDriver.instance().clusterNodeProvider().consoleCommandAsync(args[0]).thenAcceptAsync(info -> {
        // check if the player has the required permission
        if (info == null || !sender.hasPermission(info.permission())) {
          // no permission
          sender.sendMessage(serializeToString(this.management.configuration().message(
            Locale.ENGLISH,
            "command-cloud-sub-command-no-permission"
          ).replace("%command%", args[0])));
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
    for (var output : CloudNetDriver.instance().clusterNodeProvider().sendCommandLine(commandLine)) {
      sender.sendMessage(serializeToString(this.management.configuration().prefix() + output));
    }
  }
}
