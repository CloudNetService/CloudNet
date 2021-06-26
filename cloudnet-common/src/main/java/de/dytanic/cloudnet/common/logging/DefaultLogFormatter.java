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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.jetbrains.annotations.NotNull;

/**
 * The default log formatter, defines a fallback log format for all AbstractLogHandler implementations
 * <p>
 * The message format looks like:
 * <p>[15.02 23:32.56.456] INFO: Hello, world!</p>
 *
 * @see AbstractLogHandler
 */
public final class DefaultLogFormatter implements IFormatter {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM HH:mm:ss.SSS");

  @Override
  public @NotNull String format(@NotNull LogEntry logEntry) {
    StringBuilder builder = new StringBuilder();
    LoggingUtils.printStackTraceToStringBuilder(builder, logEntry.getThrowable());

    StringBuilder stringBuilder = new StringBuilder();

    for (String message : logEntry.getMessages()) {
      if (message != null) {
        stringBuilder
          .append("[")
          .append(DATE_FORMAT.format(logEntry.getTimeStamp()))
          .append("] ")
          .append(logEntry.getLogLevel().getUpperName())
          .append(": ")
          .append(message)
          .append(System.lineSeparator());
      }
    }

    stringBuilder.append(builder);
    return stringBuilder.toString();
  }
}
