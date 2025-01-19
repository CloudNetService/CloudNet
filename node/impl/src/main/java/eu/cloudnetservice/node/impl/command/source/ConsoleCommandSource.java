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

package eu.cloudnetservice.node.impl.command.source;

import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 */
@Singleton
@AutoService(services = CommandSource.class, name = "console")
public class ConsoleCommandSource implements CommandSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleCommandSource.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return "Console";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NonNull String message) {
    LOGGER.info(message);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NonNull String... messages) {
    for (var message : messages) {
      LOGGER.info(message);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendMessage(@NonNull Collection<String> messages) {
    for (var message : messages) {
      LOGGER.info(message);
    }
  }

  /**
   * @param permission the permission to check for
   * @return always true as the console is allowed to execute every command
   * @throws NullPointerException if permission is null.
   */
  @Override
  public boolean checkPermission(@NonNull String permission) {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.name();
  }
}
