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

package eu.cloudnetservice.node.command.defaults;

import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Singleton;
import java.util.concurrent.Executors;
import lombok.NonNull;
import org.incendo.cloud.CloudCapability;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.meta.CommandMeta;
import org.incendo.cloud.meta.SimpleCommandMeta;

/**
 * {@inheritDoc}
 */
@Singleton
final class DefaultCommandManager extends CommandManager<CommandSource> {

  /**
   * Constructs the default implementation of the {@link CommandManager}. Applying asynchronous command executing using
   * a thread pool with 4 threads.
   */
  private DefaultCommandManager() {
    var executor = Executors.newFixedThreadPool(4);
    super(
      ExecutionCoordinator.<CommandSource>builder()
        .parsingExecutor(executor)
        .executionSchedulingExecutor(executor)
        .build(),
      CommandRegistrationHandler.nullCommandRegistrationHandler());
    this.registerCapability(CloudCapability.StandardCapabilities.ROOT_COMMAND_DELETION);
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
