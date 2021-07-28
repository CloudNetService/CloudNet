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

package de.dytanic.cloudnet.common.log.defaults;

import de.dytanic.cloudnet.common.log.LoggingUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class DefaultLogFormatter extends Formatter {

  public static final DefaultLogFormatter INSTANCE = new DefaultLogFormatter();
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM HH:mm:ss.SSS");

  private DefaultLogFormatter() {
  }

  @Override
  public String format(LogRecord record) {
    StringBuilder builder = new StringBuilder()
      .append('[')
      .append(DATE_FORMAT.format(record.getMillis()))
      .append("] ")
      .append(record.getLevel().getLocalizedName())
      .append(": ")
      .append(super.formatMessage(record))
      .append(System.lineSeparator());
    LoggingUtils.printStackTraceInto(builder, record);

    return builder.toString();
  }
}
