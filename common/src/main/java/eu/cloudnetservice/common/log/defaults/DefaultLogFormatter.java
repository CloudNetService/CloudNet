/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.common.log.defaults;

import eu.cloudnetservice.common.log.LoggingUtil;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import lombok.NonNull;

/**
 * The default log formatter which prints out console messages plain without any special coloring. See the static fields
 * of the class to access an instance of this formatter.
 *
 * @since 4.0
 */
public final class DefaultLogFormatter extends Formatter {

  /**
   * An instance of the log formatter which does not append a line separator at the end of the formatting.
   */
  public static final DefaultLogFormatter END_CLEAN = new DefaultLogFormatter(false);
  /**
   * An instance of the log formatter which appends a line separator at the end of the formatting.
   */
  public static final DefaultLogFormatter END_LINE_SEPARATOR = new DefaultLogFormatter(true);

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss.SSS");

  private final boolean closeWithLineSeparator;

  /**
   * Constructs a new default log formatter instance.
   *
   * @param closeWithLineSeparator if the messages formatted by this handler should get the line separator appended.
   */
  private DefaultLogFormatter(boolean closeWithLineSeparator) {
    this.closeWithLineSeparator = closeWithLineSeparator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String format(@NonNull LogRecord record) {
    var builder = new StringBuilder()
      .append('[')
      .append(DATE_TIME_FORMATTER.format(record.getInstant().atZone(ZoneId.systemDefault())))
      .append("] ")
      .append(record.getLevel().getLocalizedName())
      .append(": ")
      .append(super.formatMessage(record))
      .append(this.closeWithLineSeparator ? System.lineSeparator() : "");
    LoggingUtil.printStackTraceInto(builder, record);

    return builder.toString();
  }
}
