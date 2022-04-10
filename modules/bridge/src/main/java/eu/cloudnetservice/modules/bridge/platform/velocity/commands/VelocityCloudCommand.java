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

package eu.cloudnetservice.modules.bridge.platform.velocity.commands;

import static eu.cloudnetservice.ext.adventure.AdventureSerializerUtil.serialize;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

public final class VelocityCloudCommand implements SimpleCommand {

  private final PlatformBridgeManagement<?, ?> management;

  public VelocityCloudCommand(PlatformBridgeManagement<?, ?> management) {
    this.management = management;
  }

  @Override
  public void execute(@NonNull Invocation invocation) {
    // check if any arguments are provided
    if (invocation.arguments().length == 0) {
      // <prefix> /cloudnet <command>
      invocation.source()
        .sendMessage(serialize(this.management.configuration().prefix() + "/cloudnet <command>"));
      return;
    }
    // get the full command line
    var commandLine = String.join(" ", invocation.arguments());
    // skip the permission check if the source is the console
    if (!(invocation.source() instanceof ConsoleCommandSource)) {
      // get the command info
      var command = CloudNetDriver.instance().clusterNodeProvider().consoleCommand(commandLine);
      // check if the sender has the required permission to execute the command
      if (command != null) {
        if (!invocation.source().hasPermission(command.permission())) {
          invocation.source().sendMessage(serialize(this.management.configuration().message(
            invocation.source() instanceof Player
              ? ((Player) invocation.source()).getEffectiveLocale()
              : Locale.ENGLISH,
            "command-cloud-sub-command-no-permission"
          ).replace("%command%", command.name())));
          return;
        }
      }
    }
    // execute the command
    CloudNetDriver.instance().clusterNodeProvider().sendCommandLineAsync(commandLine).thenAccept(messages -> {
      for (var line : messages) {
        invocation.source().sendMessage(serialize(this.management.configuration().prefix() + line));
      }
    });
  }

  @Override
  public @NonNull CompletableFuture<List<String>> suggestAsync(@NonNull Invocation invocation) {
    return CompletableFuture.supplyAsync(() -> List.copyOf(CloudNetDriver.instance()
      .clusterNodeProvider()
      .consoleTabCompleteResults(String.join(" ", invocation.arguments()))));
  }

  @Override
  public boolean hasPermission(@NonNull Invocation invocation) {
    return invocation.source().hasPermission("cloudnet.command.cloudnet");
  }
}
