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

package de.dytanic.cloudnet.common.log.io;

import de.dytanic.cloudnet.common.log.Logger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

public final class LogOutputStream extends ByteArrayOutputStream {

  public static boolean DEFAULT_AUTO_FLUSH = true;
  public static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final Level level;
  private final Logger logger;

  private LogOutputStream(@NotNull Level level, @NotNull Logger logger) {
    this.level = level;
    this.logger = logger;
  }

  public static @NotNull LogOutputStream forSevere(@NotNull Logger logger) {
    return LogOutputStream.newInstance(logger, Level.SEVERE);
  }

  public static @NotNull LogOutputStream forInformative(@NotNull Logger logger) {
    return LogOutputStream.newInstance(logger, Level.INFO);
  }

  public static @NotNull LogOutputStream newInstance(@NotNull Logger logger, @NotNull Level level) {
    return new LogOutputStream(level, logger);
  }

  public @NotNull PrintStream toPrintStream() {
    return this.toPrintStream(DEFAULT_AUTO_FLUSH, DEFAULT_CHARSET);
  }

  public @NotNull PrintStream toPrintStream(boolean autoFlush, @NotNull Charset charset) {
    try {
      return new PrintStream(this, autoFlush, charset.name());
    } catch (UnsupportedEncodingException exception) {
      throw new IllegalArgumentException("Charset " + charset + " is unsupported", exception);
    }
  }

  @Override
  public void flush() throws IOException {
    synchronized (this) {
      super.flush();
      var content = this.toString(StandardCharsets.UTF_8.name());
      super.reset();

      if (!content.isEmpty() && !content.equals(System.lineSeparator())) {
        this.logger.log(this.level, content);
      }
    }
  }
}
