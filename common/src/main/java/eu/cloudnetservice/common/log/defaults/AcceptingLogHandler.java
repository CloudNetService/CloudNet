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

import eu.cloudnetservice.common.log.AbstractHandler;
import java.util.function.Consumer;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import lombok.NonNull;

/**
 * A log handler which automatically formats the given log record and notifies the provided message handler.
 *
 * @since 4.0
 */
public final class AcceptingLogHandler extends AbstractHandler {

  private final Consumer<String> handler;

  /**
   * Constructs a new accepting log handler instance.
   *
   * @param handler the consumer to post loggable, formatted log records to.
   * @throws NullPointerException if the given handler is null.
   */
  private AcceptingLogHandler(@NonNull Consumer<String> handler) {
    this.handler = handler;
    // default options
    this.setLevel(Level.ALL);
  }

  /**
   * Constructs a new accepting log handler instance.
   *
   * @param handler the consumer to post loggable, formatted log records to.
   * @throws NullPointerException if the given handler is null.
   */
  public static @NonNull AcceptingLogHandler newInstance(@NonNull Consumer<String> handler) {
    return new AcceptingLogHandler(handler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void publish(LogRecord record) {
    if (super.isLoggable(record)) {
      this.handler.accept(super.getFormatter().format(record));
    }
  }

  /**
   * Sets the formatter of this handler and returns the same instance as used to call the method, for chaining.
   *
   * @param formatter the formatter to use for this handler.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given formatter is null.
   */
  public @NonNull AcceptingLogHandler withFormatter(@NonNull Formatter formatter) {
    super.setFormatter(formatter);
    return this;
  }
}
