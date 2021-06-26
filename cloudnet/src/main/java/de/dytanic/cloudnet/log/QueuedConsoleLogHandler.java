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

package de.dytanic.cloudnet.log;

import de.dytanic.cloudnet.common.logging.ILogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.event.log.LoggingEntryEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;

/**
 * A logging handler for developers, that can easy handle and get the logging outputs from this node instance
 */
public final class QueuedConsoleLogHandler implements ILogHandler {

  /**
   * A queue that contain the last 128 logging output as LogEntries that should print into the console
   */
  private final Queue<LogEntry> cachedQueuedLogEntries = new ConcurrentLinkedQueue<>();

  @Override
  public void handle(@NotNull LogEntry logEntry) {
    this.cachedQueuedLogEntries.offer(logEntry);

    while (this.cachedQueuedLogEntries.size() > 128) {
      this.cachedQueuedLogEntries.poll();
    }

    CloudNetDriver.getInstance().getEventManager().callEvent(new LoggingEntryEvent(logEntry));
  }

  @Override
  public void close() {
  }

  public Queue<LogEntry> getCachedQueuedLogEntries() {
    return this.cachedQueuedLogEntries;
  }
}
