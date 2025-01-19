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

package eu.cloudnetservice.utils.base.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * A special implementation of an output stream specifically made to log all the written content to it to a provided
 * logger instance.
 *
 * @since 4.0
 */
public final class LogOutputStream extends ByteArrayOutputStream {

  public static final boolean DEFAULT_AUTO_FLUSH = true;
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final Level level;
  private final Logger logger;
  private final Lock flushLock;

  /**
   * Creates a new log output stream instance.
   *
   * @param level  the level which should be used when logging messages.
   * @param logger the logger to which all messages should get logged.
   * @throws NullPointerException if the given level or logger is null.
   */
  private LogOutputStream(@NonNull Level level, @NonNull Logger logger) {
    this.level = level;
    this.logger = logger;
    this.flushLock = new ReentrantLock();
  }

  /**
   * Creates a new log output stream instance logging all messages to the given logger with the severe log level.
   *
   * @param logger the logger to log all messages to.
   * @return a new log output stream instance for the given logger.
   * @throws NullPointerException if the given logger is null.
   */
  public static @NonNull LogOutputStream forWarn(@NonNull Logger logger) {
    return LogOutputStream.newInstance(logger, Level.WARN);
  }

  /**
   * Creates a new log output stream instance logging all messages to the given logger with the info log level.
   *
   * @param logger the logger to log all messages to.
   * @return a new log output stream instance for the given logger.
   * @throws NullPointerException if the given logger is null.
   */
  public static @NonNull LogOutputStream forInfo(@NonNull Logger logger) {
    return LogOutputStream.newInstance(logger, Level.INFO);
  }

  /**
   * Creates a new log output stream instance logging all messages to the given logger using the given log level.
   *
   * @param logger the logger to log all messages to.
   * @param level  the level which the messages should have when logging them.
   * @return a new log output stream instance for the given logger and level.
   * @throws NullPointerException if the given logger or level is null.
   */
  public static @NonNull LogOutputStream newInstance(@NonNull Logger logger, @NonNull Level level) {
    return new LogOutputStream(level, logger);
  }

  /**
   * Wraps this log output stream into a print stream which automatically flushes and uses the UTF-8 charset encoding.
   *
   * @return a print stream wrapping this logging output stream.
   */
  public @NonNull PrintStream toPrintStream() {
    return this.toPrintStream(DEFAULT_AUTO_FLUSH, DEFAULT_CHARSET);
  }

  /**
   * Wraps this log output stream in a print stream using the given auto flush and charset settings.
   *
   * @param autoFlush if this buffer should be flushed automatically if content was written to the print stream.
   * @param charset   the charset to use when flushing out buffer content.
   * @return a print stream wrapping this output stream.
   * @throws NullPointerException if the given charset is null.
   */
  public @NonNull PrintStream toPrintStream(boolean autoFlush, @NonNull Charset charset) {
    return new PrintStream(this, autoFlush, charset);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws IOException {
    // ensure that we only flush once at a time
    this.flushLock.lock();
    try {
      super.flush();
      var content = this.toString(StandardCharsets.UTF_8).stripTrailing();
      super.reset();

      if (!content.isEmpty()) {
        this.logger.atLevel(this.level).log(content);
      }
    } finally {
      this.flushLock.unlock();
    }
  }
}
