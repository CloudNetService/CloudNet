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

package eu.cloudnetservice.wrapper.log;

import eu.cloudnetservice.common.log.AbstractHandler;
import java.io.PrintStream;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A redirect log handler, to the origin output and error stream. The LogOutputStream will replace the System.out and
 * System.err stream after this initialization by the Wrapper
 */
@ApiStatus.Internal
public final class InternalPrintStreamLogHandler extends AbstractHandler {

  private final PrintStream outputStream;
  private final PrintStream errorStream;

  private InternalPrintStreamLogHandler(@NonNull PrintStream outputStream, @NonNull PrintStream errorStream) {
    this.outputStream = outputStream;
    this.errorStream = errorStream;
    // set defaults
    super.setLevel(Level.ALL);
  }

  public static @NonNull InternalPrintStreamLogHandler forSystemStreams() {
    return InternalPrintStreamLogHandler.newInstance(System.out, System.err);
  }

  public static @NonNull InternalPrintStreamLogHandler newInstance(@NonNull PrintStream out, @NonNull PrintStream err) {
    return new InternalPrintStreamLogHandler(out, err);
  }

  @Override
  public void publish(@NonNull LogRecord record) {
    var stream = record.getLevel().intValue() > Level.INFO.intValue() ? this.errorStream : this.outputStream;
    stream.println(super.getFormatter().format(record));
  }

  public @NonNull InternalPrintStreamLogHandler withFormatter(@NonNull Formatter formatter) {
    super.setFormatter(formatter);
    return this;
  }
}
