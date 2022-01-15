/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.common.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import lombok.NonNull;

/**
 * Util for the {@link Logger}
 */
public final class LoggingUtil {

  private LoggingUtil() {
    throw new UnsupportedOperationException();
  }

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

  public static void removeHandlers(@NonNull Logger logger) {
    for (var handler : logger.getHandlers()) {
      logger.removeHandler(handler);
    }
  }

  public static void printStackTraceInto(@NonNull StringBuilder stringBuilder, @NonNull LogRecord record) {
    if (record.getThrown() != null) {
      var writer = new StringWriter();
      record.getThrown().printStackTrace(new PrintWriter(writer));
      stringBuilder.append('\n').append(writer);
    }
  }
}
