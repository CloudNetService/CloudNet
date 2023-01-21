/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.command.sub;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Confirmation;
import eu.cloudnetservice.node.ShutdownHandler;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@CommandAlias({"shutdown", "stop"})
@CommandPermission("cloudnet.command.exit")
@Description("command-exit-description")
public final class ExitCommand {

  private final Provider<ShutdownHandler> shutdownHandlerProvider;

  @Inject
  public ExitCommand(@NonNull Provider<ShutdownHandler> shutdownHandlerProvider) {
    this.shutdownHandlerProvider = shutdownHandlerProvider;
  }

  @Confirmation
  @CommandMethod(value = "exit|shutdown|stop", requiredSender = ConsoleCommandSource.class)
  public void exit() {
    // call our shutdown handler, this prevents the Cleaner shutdown hook which is added
    // by java to run first. The cleaner task will close all logging providers, which makes it
    // impossible for us to log any messages while shutting down
    var shutdownHandler = this.shutdownHandlerProvider.get();
    shutdownHandler.shutdown();
  }
}
