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

package eu.cloudnetservice.common.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import lombok.NonNull;

/**
 * Holds some utility methods to work with loggers.
 *
 * @since 4.0
 */
public final class LoggingUtil {

  private LoggingUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the default log level configured for the CloudNet runtime using the {@code cloudnet.logging.defaultlevel}
   * system property. If no level is set or the level cannot be parsed from the provided string this method falls back
   * to the info log level.
   *
   * @return the configured log level of the system, info if not or an invalid value was set.
   */
  public static @NonNull Level defaultLogLevel() {
    var defaultLogLevel = System.getProperty("cloudnet.logging.defaultlevel");
    if (defaultLogLevel == null) {
      return Level.INFO;
    } else {
      try {
        return Level.parse(defaultLogLevel);
      } catch (IllegalArgumentException exception) {
        // no such log level or level number, default to INFO
        return Level.INFO;
      }
    }
  }

  /**
   * Removes all registered handlers from the given logger.
   *
   * @param logger the logger to remove the handlers of.
   * @throws NullPointerException if the given logger is null.
   */
  public static void removeHandlers(@NonNull Logger logger) {
    for (var handler : logger.getHandlers()) {
      logger.removeHandler(handler);
    }
  }

  /**
   * Prints the throwable of the log record into the given string builder. If the given record has no associated
   * exception set this method does nothing.
   *
   * @param stringBuilder the string builder to print the exception to.
   * @param record        the record of which the exception should get printed into the builder.
   * @throws NullPointerException if the given string builder or log record is null.
   */
  public static void printStackTraceInto(@NonNull StringBuilder stringBuilder, @NonNull LogRecord record) {
    if (record.getThrown() != null) {
      var writer = new StringWriter();
      record.getThrown().printStackTrace(new PrintWriter(writer));
      stringBuilder.append('\n').append(writer);
    }
  }
}
