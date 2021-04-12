package de.dytanic.cloudnet.log;

import de.dytanic.cloudnet.common.logging.ILogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.event.log.LoggingEntryEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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