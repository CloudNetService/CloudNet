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
