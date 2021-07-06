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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogEntryDispatcher extends Thread {

  private final ILogger parentLogger;
  private final BlockingQueue<LogEntry> queue = new LinkedBlockingQueue<>();

  public LogEntryDispatcher(ILogger parentLogger) {
    super("Log record dispatcher thread");
    this.setPriority(Thread.MIN_PRIORITY);

    this.parentLogger = parentLogger;
  }

  @Override
  public void run() {
    while (!super.isInterrupted()) {
      try {
        this.dispatchLogEntry(this.queue.take());
      } catch (InterruptedException exception) {
        break;
      }
    }

    for (LogEntry logEntry : this.queue) {
      this.dispatchLogEntry(logEntry);
    }
  }

  protected void enqueueLogEntry(LogEntry entry) {
    if (!super.isInterrupted()) {
      this.queue.add(entry);
    }
  }

  protected void dispatchLogEntry(LogEntry entry) {
    for (ILogHandler logHandler : this.parentLogger.getLogHandlers()) {
      try {
        logHandler.handle(entry);
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }
  }
}
