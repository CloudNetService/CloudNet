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

package eu.cloudnetservice.node.command.source;

import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.util.Collection;
import lombok.NonNull;

/**
 * The command source represents a message receiving object. All messages regarding command execution and command
 * parsing are sent to the command source.
 * <p>
 * The console has its own CommandSource. If you want to use the console CommandSource use the jvm static
 * {@link CommandSource#console()} method.
 *
 * @since 4.0
 */
public interface CommandSource extends Named {

  static @NonNull CommandSource console() {
    return ServiceRegistry.registry().instance(CommandSource.class, "console");
  }

  /**
   * @param message the message that is sent to the source
   * @throws NullPointerException if message is null.
   */
  void sendMessage(@NonNull String message);

  /**
   * @param messages the messages that are sent to the source
   * @throws NullPointerException if messages is null.
   */
  void sendMessage(@NonNull String... messages);

  /**
   * @param messages the messages that are sent to the source
   * @throws NullPointerException if messages is null.
   */
  void sendMessage(@NonNull Collection<String> messages);

  /**
   * Used to check if the command source has the given permission
   *
   * @param permission the permission to check for
   * @return whether the source has the permission
   * @throws NullPointerException if permission is null.
   */
  boolean checkPermission(@NonNull String permission);
}
