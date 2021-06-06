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

package de.dytanic.cloudnet.command;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;

/**
 * The ConsoleCommandSender represents the console of the application. The console has all needed permissions.
 */
public final class ConsoleCommandSender implements ICommandSender {

  private final ILogger logger;

  public ConsoleCommandSender(ILogger logger) {
    this.logger = logger;
  }

  /**
   * The console name is the codename of the current CloudNet version
   */
  @Override
  public String getName() {
    return "Earthquake";
  }

  @Override
  public void sendMessage(String message) {
    this.logger.log(LogLevel.COMMAND, message);
  }

  @Override
  public void sendMessage(String... messages) {
    Preconditions.checkNotNull(messages);

    for (String message : messages) {
      this.sendMessage(message);
    }
  }

  /**
   * The console as always the permission for by every request
   */
  @Override
  public boolean hasPermission(String permission) {
    return true;
  }
}
