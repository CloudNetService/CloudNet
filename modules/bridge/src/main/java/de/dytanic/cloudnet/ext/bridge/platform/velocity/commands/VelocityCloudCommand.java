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

package de.dytanic.cloudnet.ext.bridge.platform.velocity.commands;

import static eu.cloudnetservice.ext.adventure.AdventureSerializerUtil.serialize;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public final class VelocityCloudCommand implements SimpleCommand {

  private final PlatformBridgeManagement<?, ?> management;

  public VelocityCloudCommand(PlatformBridgeManagement<?, ?> management) {
    this.management = management;
  }

  @Override
  public void execute(@NotNull Invocation invocation) {
    // check if any arguments are provided
    if (invocation.arguments().length == 0) {
      // <prefix> /cloudnet <command>
      invocation.source()
        .sendMessage(serialize(this.management.getConfiguration().getPrefix() + "/cloudnet <command>"));
      return;
    }
    // get the full command line
    String commandLine = String.join(" ", invocation.arguments());
    // skip the permission check if the source is the console
    if (!(invocation.source() instanceof ConsoleCommandSource)) {
      // get the command info
      CommandInfo command = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);
      // check if the sender has the required permission to execute the command
      if (command != null && command.getPermission() != null) {
        if (!invocation.source().hasPermission(command.getPermission())) {
          invocation.source().sendMessage(serialize(this.management.getConfiguration().getMessage(
            invocation.source() instanceof Player
              ? ((Player) invocation.source()).getEffectiveLocale()
              : Locale.ENGLISH,
            "command-cloud-sub-command-no-permission"
          ).replace("%command%", command.getName())));
          return;
        }
      }
    }
    // execute the command
    CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLineAsync(commandLine).onComplete(messages -> {
      for (String line : messages) {
        invocation.source().sendMessage(serialize(this.management.getConfiguration().getPrefix() + line));
      }
    });
  }

  @Override
  public @NotNull CompletableFuture<List<String>> suggestAsync(@NotNull Invocation invocation) {
    return CompletableFuture.supplyAsync(() -> ImmutableList.copyOf(CloudNetDriver.getInstance()
      .getNodeInfoProvider()
      .getConsoleTabCompleteResults(String.join(" ", invocation.arguments()))));
  }

  @Override
  public boolean hasPermission(@NotNull Invocation invocation) {
    return invocation.source().hasPermission("cloudnet.command.cloudnet");
  }
}
