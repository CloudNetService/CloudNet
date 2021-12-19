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

import de.dytanic.cloudnet.common.INameable;
import java.util.Collection;
import lombok.NonNull;

public interface CommandSource extends INameable {

  /**
   * @return the console command source instance
   */
  static CommandSource console() {
    return ConsoleCommandSource.INSTANCE;
  }

  /**
   * @param message the message that is sent to the source
   */
  void sendMessage(@NonNull String message);

  /**
   * @param messages the messages that are sent to the source
   */
  void sendMessage(@NonNull String... messages);

  /**
   * @param messages the messages that are sent to the source
   */
  void sendMessage(@NonNull Collection<String> messages);

  /**
   * Used to check if the command source has the given permission
   *
   * @param permission the permission to check for
   * @return whether the source has the permission
   */
  boolean checkPermission(@NonNull String permission);

}
