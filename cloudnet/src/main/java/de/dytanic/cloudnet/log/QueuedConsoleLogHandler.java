package de.dytanic.cloudnet.log;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.logging.ILogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.event.log.LoggingEntryEvent;
import java.util.Queue;
import lombok.Getter;

/**
 * A logging handler for developers, that can easy handle and get the logging
 * outputs from this node instance
 */
@Getter
public final class QueuedConsoleLogHandler implements ILogHandler {

  /**
   * A queue that contain the last 128 logging output as LogEntries that should
   * print into the console
   */
  private final Queue<LogEntry> cachedQueuedLogEntries = Iterables
    .newConcurrentLinkedQueue();

  @Override
  public void handle(LogEntry logEntry) {
    cachedQueuedLogEntries.offer(logEntry);

    while (cachedQueuedLogEntries.size() > 128) {
      cachedQueuedLogEntries.poll();
    }

    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new LoggingEntryEvent(logEntry));
  }

  @Override
  public void close() {
  }
}