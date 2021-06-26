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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default implementation of the ILogger interface. The the logger executes the registered logHandlers
 * asynchronously by default or synchronously if the LogLevel disallow async log handling
 */
public class DefaultAsyncLogger implements ILogger {

  protected final AtomicInteger level = new AtomicInteger(-1);
  protected final Collection<ILogHandler> handlers = new CopyOnWriteArraySet<>();
  protected final LogEntryDispatcher dispatcher = new LogEntryDispatcher(this);

  public DefaultAsyncLogger() {
    this.dispatcher.start();
  }

  @Override
  public int getLevel() {
    return this.level.get();
  }

  @Override
  public void setLevel(int level) {
    this.level.set(level);
  }

  @Override
  public ILogger log(LogEntry logEntry) {
    this.handleLogEntry(logEntry);
    return this;
  }

  @Override
  public ILogger log(LogEntry... logEntries) {
    for (LogEntry logEntry : logEntries) {
      this.handleLogEntry(logEntry);
    }

    return this;
  }

  @Override
  public boolean hasAsyncSupport() {
    return true;
  }

  @Override
  public synchronized ILogger addLogHandler(ILogHandler logHandler) {
    this.handlers.add(logHandler);
    return this;
  }

  @Override
  public synchronized ILogger addLogHandlers(ILogHandler... logHandlers) {
    for (ILogHandler logHandler : logHandlers) {
      this.addLogHandler(logHandler);
    }
    return this;
  }

  @Override
  public synchronized ILogger addLogHandlers(Iterable<ILogHandler> logHandlers) {
    for (ILogHandler logHandler : logHandlers) {
      this.addLogHandler(logHandler);
    }
    return this;
  }

  @Override
  public synchronized ILogger removeLogHandler(ILogHandler logHandler) {
    this.handlers.remove(logHandler);
    return this;
  }

  @Override
  public synchronized ILogger removeLogHandlers(ILogHandler... logHandlers) {
    for (ILogHandler logHandler : logHandlers) {
      this.removeLogHandler(logHandler);
    }
    return this;
  }

  @Override
  public synchronized ILogger removeLogHandlers(Iterable<ILogHandler> logHandlers) {
    for (ILogHandler logHandler : logHandlers) {
      this.removeLogHandler(logHandler);
    }
    return this;
  }

  @Override
  public Iterable<ILogHandler> getLogHandlers() {
    return this.handlers;
  }

  @Override
  public boolean hasLogHandler(ILogHandler logHandler) {
    return this.handlers.contains(logHandler);
  }

  @Override
  public boolean hasLogHandlers(ILogHandler... logHandlers) {
    for (ILogHandler logHandler : logHandlers) {
      if (!this.handlers.contains(logHandler)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() throws Exception {
    for (ILogHandler logHandler : this.handlers) {
      logHandler.close();
    }

    this.dispatcher.interrupt();
    this.dispatcher.join();

    this.handlers.clear();
  }

  private void handleLogEntry(LogEntry logEntry) {
    if (logEntry != null && (this.getLevel() == -1 || logEntry.getLogLevel().getLevel() <= this.getLevel())) {
      if (logEntry.getLogLevel().isAsync()) {
        this.dispatcher.enqueueLogEntry(logEntry);
      } else {
        this.dispatcher.dispatchLogEntry(logEntry);
      }
    }
  }
}
