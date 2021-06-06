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

package de.dytanic.cloudnet.ext.bridge.velocity.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.ext.bridge.velocity.util.VelocityComponentRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CommandCloudNet implements SimpleCommand {

  @Override
  public void execute(Invocation invocation) {
    String[] arguments = invocation.arguments();
    if (arguments.length == 0) {
      invocation.source().sendMessage(VelocityComponentRenderer.prefixed("/cloudnet <command>"));
      return;
    }

    String commandLine = String.join(" ", arguments);
    if (invocation.source() instanceof Player) {
      Player player = (Player) invocation.source();

      CommandInfo commandInfo = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);
      if (commandInfo != null && commandInfo.getPermission() != null) {
        if (!player.hasPermission(commandInfo.getPermission())) {
          player.sendMessage(VelocityComponentRenderer.prefixedTranslation("command-cloud-sub-command-no-permission",
            message -> message.replace("%command%", commandLine)));
          return;
        }
      }
    }

    CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLineAsync(commandLine).onComplete(messages -> {
      for (String message : messages) {
        if (message != null) {
          invocation.source().sendMessage(VelocityComponentRenderer.prefixed(message));
        }
      }
    });
  }

  @Override
  public List<String> suggest(Invocation invocation) {
    return this.suggestAsync(invocation).join();
  }

  @Override
  public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
    return CompletableFuture.supplyAsync(() -> {
      String commandLine = String.join(" ", invocation.arguments());

      if (invocation.source() instanceof Player) {
        CommandSource source = invocation.source();

        if (commandLine.isEmpty() || commandLine.indexOf(' ') == -1) {
          List<String> responses = new ArrayList<>();
          for (CommandInfo commandInfo : CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommands()) {
            if (commandInfo.getPermission() == null || source.hasPermission(commandInfo.getPermission())) {
              responses.addAll(Arrays.asList(commandInfo.getNames()));
            }
          }
          return responses;
        }

        CommandInfo commandInfo = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);
        if (commandInfo != null && commandInfo.getPermission() != null) {
          if (!source.hasPermission(commandInfo.getPermission())) {
            return ImmutableList.of();
          }
        }
      }

      return ImmutableList
        .copyOf(CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleTabCompleteResults(commandLine));
    });
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return invocation.source().hasPermission("cloudnet.command.cloudnet");
  }
}
