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
 * The Formatter is a simply abstract way to format a LogEntry to a easy, formatted, readable message.
 */
public interface IFormatter {

  /**
   * Formats a logEntry into a readable text
   *
   * @param logEntry the log item which should be format
   * @return the new formatted string
   */
  @NotNull String format(@NotNull LogEntry logEntry);

}
