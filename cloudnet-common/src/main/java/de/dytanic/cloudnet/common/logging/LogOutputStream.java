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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Defines a new ByteArrayOutputStream that convert the bytes into a message and invokes the in constructor exist
 * logger
 */
public class LogOutputStream extends ByteArrayOutputStream {

  /**
   * The logger for this outputStream
   */
  protected final ILogger logger;

  /**
   * The LogLevel in that the logger should log the incoming message
   */
  protected final LogLevel logLevel;

  public LogOutputStream(ILogger logger, LogLevel logLevel) {
    this.logger = logger;
    this.logLevel = logLevel;
  }

  public LogOutputStream(int size, ILogger logger, LogLevel logLevel) {
    super(size);
    this.logger = logger;
    this.logLevel = logLevel;
  }

  public ILogger getLogger() {
    return this.logger;
  }

  public LogLevel getLogLevel() {
    return this.logLevel;
  }

  @Override
  public void flush() throws IOException {
    String input = this.toString(StandardCharsets.UTF_8.name());
    this.reset();

    if (input != null && !input.isEmpty() && !input.equals(System.lineSeparator())) {
      this.logger.log(this.logLevel, input);
    }
  }
}
