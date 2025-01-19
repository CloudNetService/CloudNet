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

package eu.cloudnetservice.modules.bridge.impl.platform.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

@Singleton
public final class VelocityCloudCommand implements SimpleCommand {

  private final ClusterNodeProvider clusterNodeProvider;
  private final PlatformBridgeManagement<?, ?> management;

  @Inject
  public VelocityCloudCommand(
    @NonNull ClusterNodeProvider clusterNodeProvider,
    @NonNull PlatformBridgeManagement<?, ?> management
  ) {
    this.clusterNodeProvider = clusterNodeProvider;
    this.management = management;
  }

  @Override
  public void execute(@NonNull Invocation invocation) {
    // check if any arguments are provided
    var arguments = invocation.arguments();
    if (arguments.length == 0) {
      // <prefix> /cloudnet <command>
      invocation.source().sendMessage(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(
        this.management.configuration().prefix() + "/cloudnet <command>"));
      return;
    }
    // get the full command line
    var commandLine = String.join(" ", arguments);
    // skip the permission check if the source is the console
    if (invocation.source() instanceof ConsoleCommandSource) {
      // execute the command
      this.executeNow(invocation.source(), commandLine);
    } else {
      // get the command info
      this.clusterNodeProvider.consoleCommandAsync(arguments[0]).thenAcceptAsync(info -> {
        // check if the sender has the required permission to execute the command
        if (info == null || !invocation.source().hasPermission(info.permission())) {
          // no permission to execute the command
          this.management.configuration().handleMessage(
            invocation.source() instanceof Player player ? player.getEffectiveLocale() : Locale.ENGLISH,
            "command-cloud-sub-command-no-permission",
            message -> ComponentFormats.BUNGEE_TO_ADVENTURE.convert(message.replace("%command%", arguments[0])),
            invocation.source()::sendMessage);
        } else {
          // execute the command
          this.executeNow(invocation.source(), commandLine);
        }
      });
    }
  }

  private void executeNow(@NonNull CommandSource source, @NonNull String commandLine) {
    for (var output : this.clusterNodeProvider.sendCommandLine(commandLine)) {
      source.sendMessage(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(
        this.management.configuration().prefix() + output));
    }
  }

  @Override
  public @NonNull CompletableFuture<List<String>> suggestAsync(@NonNull Invocation invocation) {
    return this.clusterNodeProvider
      .consoleTabCompleteResultsAsync(String.join(" ", invocation.arguments()))
      .thenApply(List::copyOf);
  }

  @Override
  public boolean hasPermission(@NonNull Invocation invocation) {
    return invocation.source().hasPermission("cloudnet.command.cloudnet");
  }
}
