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

package de.dytanic.cloudnet.console.log;

import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.common.logging.LoggingUtils;
import de.dytanic.cloudnet.console.ConsoleColor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.jetbrains.annotations.NotNull;

public final class ColouredLogFormatter implements IFormatter {

  private final DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss.SSS");

  @Override
  public @NotNull String format(@NotNull LogEntry logEntry) {
    StringBuilder builder = new StringBuilder();
    LoggingUtils.printStackTraceToStringBuilder(builder, logEntry.getThrowable());

    StringBuilder stringBuilder = new StringBuilder();

    for (String message : logEntry.getMessages()) {
      if (message != null) {
        stringBuilder
          .append(ConsoleColor.DARK_GRAY)
          .append("[")
          .append(ConsoleColor.WHITE)
          .append(this.dateFormat.format(logEntry.getTimeStamp()))
          .append(ConsoleColor.DARK_GRAY)
          .append("] ")
          .append(logEntry.getLogLevel().isColorized() ? ConsoleColor.RED : ConsoleColor.GRAY)
          .append(logEntry.getLogLevel().getUpperName())
          .append(ConsoleColor.DARK_GRAY)
          .append(": ")
          .append(logEntry.getLogLevel().isColorized() ? ConsoleColor.YELLOW : ConsoleColor.DEFAULT)
          .append(message);
      }
    }

    return stringBuilder.append(builder).toString();
  }
}
