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

package de.dytanic.cloudnet.command.source;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class ConsoleCommandSource implements CommandSource {

  private static final Logger LOGGER = LogManager.getLogger(ConsoleCommandSource.class);
  public static final ConsoleCommandSource INSTANCE = new ConsoleCommandSource();

  /**
   * @return "Console" for the console command source.
   */
  @Override
  public @NotNull String getName() {
    return "Console";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NotNull String message) {
    LOGGER.info(message);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NotNull String... messages) {
    for (String message : messages) {
      LOGGER.info(message);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NotNull Collection<String> messages) {
    for (String message : messages) {
      LOGGER.info(message);
    }
  }

  /**
   * @param permission the permission to check for
   * @return always {@code true} as the console is allowed to execute every command
   */
  @Override
  public boolean checkPermission(@NotNull String permission) {
    return true;
  }
}
