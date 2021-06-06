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

package de.dytanic.cloudnet.common.logging;

import org.jetbrains.annotations.NotNull;

/**
 * A LogHandler should handle an incoming LogEntry. The Operation can be execute asynchronously or synchronously. It
 * depends on the implementation of the logger and the setting of the LogLevel
 */
public interface ILogHandler extends AutoCloseable {

  /**
   * Allows to handle this incoming LogEntry from the logger This method can invoked asynchronously or synchronously. It
   * depends on the implementation of the logger and the setting of the LogLevel
   *
   * @param logEntry the new incoming log entry
   */
  void handle(@NotNull LogEntry logEntry) throws Exception;

}
