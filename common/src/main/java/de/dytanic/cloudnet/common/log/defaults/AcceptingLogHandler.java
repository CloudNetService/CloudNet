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

import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.jetbrains.annotations.NotNull;

public final class AcceptingLogHandler extends Handler {

  private final Consumer<String> accepter;

  private AcceptingLogHandler(Consumer<String> accepter) {
    this.accepter = accepter;
    // default options
    this.setLevel(Level.ALL);
  }

  public static @NotNull AcceptingLogHandler newInstance(@NotNull Consumer<String> accepter) {
    return new AcceptingLogHandler(Preconditions.checkNotNull(accepter, "accepter"));
  }

  @Override
  public void publish(LogRecord record) {
    if (super.isLoggable(record)) {
      this.accepter.accept(super.getFormatter().format(record));
    }
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() throws SecurityException {
  }

  public @NotNull AcceptingLogHandler withFormatter(@NotNull Formatter formatter) {
    super.setFormatter(formatter);
    return this;
  }
}
