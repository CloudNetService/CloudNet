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

import de.dytanic.cloudnet.common.log.LoggingUtils;
import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.console.ConsoleColor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

public final class ColouredLogFormatter extends Formatter implements IFormatter {

  private final DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss.SSS");

  @Override
  public String format(LogRecord record) {
    StringBuilder builder = new StringBuilder()
      .append(ConsoleColor.DARK_GRAY)
      .append('[')
      .append(ConsoleColor.WHITE)
      .append(this.dateFormat.format(record.getMillis()))
      .append(ConsoleColor.DARK_GRAY)
      .append("] ")
      .append(this.getColor(record.getLevel()))
      .append(ConsoleColor.DARK_GRAY)
      .append(": ")
      .append(ConsoleColor.DEFAULT)
      .append(super.formatMessage(record))
      .append(System.lineSeparator());
    LoggingUtils.printStackTraceInto(builder, record);

    return builder.toString();
  }

  private @NotNull String getColor(@NotNull Level level) {
    ConsoleColor color = ConsoleColor.DARK_GRAY;
    if (level == Level.INFO) {
      color = ConsoleColor.GREEN;
    } else if (level == Level.WARNING) {
      color = ConsoleColor.YELLOW;
    } else if (level == Level.SEVERE) {
      color = ConsoleColor.RED;
    } else if (level.intValue() >= Level.FINEST.intValue() && level.intValue() <= Level.FINE.intValue()) {
      color = ConsoleColor.BLUE;
    }

    return color + level.getLocalizedName();
  }

  @Override
  @Deprecated
  @ScheduledForRemoval
  public @NotNull String format(@NotNull LogEntry logEntry) {
    StringBuilder builder = new StringBuilder();
    de.dytanic.cloudnet.common.logging.LoggingUtils.printStackTraceToStringBuilder(builder, logEntry.getThrowable());

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
