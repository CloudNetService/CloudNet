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

package eu.cloudnetservice.cloudnet.node.command.defaults;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.internal.CommandRegistrationHandler;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import java.util.concurrent.Executors;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * {@inheritDoc}
 */
final class DefaultCommandManager extends CommandManager<CommandSource> {

  /**
   * Constructs the default implementation of the {@link CommandManager}. Applying asynchronous command executing using
   * a thread pool with 4 threads.
   */
  public DefaultCommandManager() {
    super(AsynchronousCommandExecutionCoordinator.<CommandSource>newBuilder()
        .withExecutor(Executors.newFixedThreadPool(4)).build(),
      CommandRegistrationHandler.nullCommandRegistrationHandler());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasPermission(@NonNull CommandSource sender, @NonNull String permission) {
    return sender.checkPermission(permission);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CommandMeta createDefaultCommandMeta() {
    return SimpleCommandMeta.empty();
  }
}
