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

package de.dytanic.cloudnet.wrapper.log;

import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.common.logging.LogLevel;
import java.io.PrintStream;
import org.jetbrains.annotations.NotNull;

/**
 * A redirect log handler, to the origin output and error stream. The LogOutputStream will replace the System.out and
 * System.err stream after this initialization by the Wrapper
 */
public final class InternalPrintStreamLogHandler extends AbstractLogHandler {

  private final PrintStream outputStream;
  private final PrintStream errorStream;

  public InternalPrintStreamLogHandler(PrintStream outputStream, PrintStream errorStream) {
    this.outputStream = outputStream;
    this.errorStream = errorStream;
  }

  @Override
  public void handle(@NotNull LogEntry logEntry) {
    PrintStream targetStream =
      logEntry.getLogLevel().equals(LogLevel.ERROR) || logEntry.getLogLevel().equals(LogLevel.WARNING)
        ? this.errorStream
        : this.outputStream;

    for (String line : super.getFormatter().format(logEntry).split(System.lineSeparator())) {
      targetStream.println(line);
    }
  }

  @Override
  public void close() {

  }
}
