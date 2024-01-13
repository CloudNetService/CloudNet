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

package eu.cloudnetservice.node.log;

import eu.cloudnetservice.common.log.AbstractHandler;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.node.event.log.LoggingEntryEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * A logging handler for developers, that can easy handle and get the logging outputs from this node instance
 */
@Singleton
public final class QueuedConsoleLogHandler extends AbstractHandler {

  private final EventManager eventManager;

  /**
   * A queue that contain the last 128 logging output as LogEntries that should print into the console
   */
  private final Queue<LogRecord> cachedQueuedLogEntries = new ConcurrentLinkedQueue<>();

  @Inject
  public QueuedConsoleLogHandler(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  @Override
  public void publish(@NonNull LogRecord record) {
    this.cachedQueuedLogEntries.offer(record);
    while (this.cachedQueuedLogEntries.size() > 128) {
      this.cachedQueuedLogEntries.poll();
    }

    this.eventManager.callEvent(new LoggingEntryEvent(record));
  }

  public @NonNull Queue<LogRecord> cachedLogEntries() {
    return this.cachedQueuedLogEntries;
  }

  public @NonNull Queue<String> formattedCachedLogLines() {
    return this.cachedQueuedLogEntries.stream()
      .map(this.getFormatter()::format)
      .collect(Collectors.toCollection(LinkedList::new));
  }
}
