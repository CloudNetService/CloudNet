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

package de.dytanic.cloudnet.command.defaults;

import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.internal.CommandRegistrationHandler;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import de.dytanic.cloudnet.command.source.CommandSource;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;

public class DefaultCommandManager extends CommandManager<CommandSource> {

  /**
   * Create a new command manager instance
   *
   * @param commandExecutionCoordinator Execution coordinator instance. The coordinator is in charge of executing
   *                                    incoming commands. Some considerations must be made when picking a suitable
   *                                    execution coordinator for your platform. For example, an entirely asynchronous
   *                                    coordinator is not suitable when the parsers used in that particular platform
   *                                    are not thread safe. If you have commands that perform blocking operations,
   *                                    however, it might not be a good idea to use a synchronous execution coordinator.
   *                                    In most cases you will want to pick between {@link CommandExecutionCoordinator#simpleCoordinator()}
   *                                    and {@link AsynchronousCommandExecutionCoordinator}
   * @param commandRegistrationHandler  Command registration handler. This will get called every time a new command is
   *                                    registered to the command manager. This may be used to forward command
   *                                    registration
   */
  protected DefaultCommandManager(
    @NonNull Function<@NonNull CommandTree<CommandSource>, @NonNull CommandExecutionCoordinator<CommandSource>> commandExecutionCoordinator,
    @NonNull CommandRegistrationHandler commandRegistrationHandler) {
    super(commandExecutionCoordinator, commandRegistrationHandler);
  }

  @Override
  public boolean hasPermission(@NonNull CommandSource sender, @NonNull String permission) {
    return sender.checkPermission(permission);
  }

  @Override
  public @NonNull CommandMeta createDefaultCommandMeta() {
    return SimpleCommandMeta.empty();
  }
}
