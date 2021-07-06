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

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public final class CommandCloudNet extends Command implements TabExecutor {

  public CommandCloudNet() {
    super("cloudnet", "cloudnet.command.cloudnet", "cloud", "cl");
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      sender.sendMessage(TextComponent.fromLegacyText(
        ChatColor.translateAlternateColorCodes('&', BridgeConfigurationProvider.load().getPrefix())
          + "/cloudnet <command>"));
      return;
    }

    String commandLine = String.join(" ", args);

    if (sender instanceof ProxiedPlayer) {
      CommandInfo commandInfo = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);

      if (commandInfo != null && commandInfo.getPermission() != null) {
        if (!sender.hasPermission(commandInfo.getPermission())) {
          sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
            BridgeConfigurationProvider.load().getMessages().get("command-cloud-sub-command-no-permission")
              .replace("%command%", commandLine))));
          return;
        }
      }
    }

    CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLineAsync(commandLine).onComplete(messages -> {
      for (String message : messages) {
        if (message != null) {
          sender.sendMessage(TextComponent.fromLegacyText(
            ChatColor.translateAlternateColorCodes('&', BridgeConfigurationProvider.load().getPrefix() + message)));
        }
      }
    });
  }

  @Override
  public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
    String commandLine = String.join(" ", args);

    if (sender instanceof ProxiedPlayer) {

      if (commandLine.isEmpty() || commandLine.indexOf(' ') == -1) {
        Collection<String> responses = new ArrayList<>();
        for (CommandInfo commandInfo : CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommands()) {
          if (commandInfo.getPermission() == null || sender.hasPermission(commandInfo.getPermission())) {
            responses.addAll(Arrays.asList(commandInfo.getNames()));
          }
        }
        return responses;
      }

      CommandInfo commandInfo = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);

      if (commandInfo != null && commandInfo.getPermission() != null) {
        if (!sender.hasPermission(commandInfo.getPermission())) {
          return Collections.emptyList();
        }
      }

    }

    return CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleTabCompleteResults(commandLine);
  }
}
