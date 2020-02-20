package de.dytanic.cloudnet.common.logging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The default implementation of the ILogger interface.
 * The the logger executes the registered logHandlers asynchronously by default or
 * synchronously if the LogLevel disallow async log handling
 */
public class DefaultAsyncLogger implements ILogger {

    protected final Collection<ILogHandler> handlers = new ArrayList<>();
    private final BlockingQueue<LogHandlerRunnable> entries = new LinkedBlockingQueue<>();
    private final Thread logThread = new Thread() {

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    LogHandlerRunnable logHandlerRunnable = entries.take();
                    logHandlerRunnable.call();

                } catch (Throwable e) {
                    break;
                }
            }

            while (!entries.isEmpty()) {
                entries.poll().call();
            }
        }
    };
    protected int level = -1;

    public DefaultAsyncLogger() {
        logThread.setPriority(Thread.MIN_PRIORITY);
        logThread.start();
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public ILogger log(LogEntry logEntry) {
        handleLogEntry(logEntry);

        return this;
    }

    @Override
    public ILogger log(LogEntry... logEntries) {
        for (LogEntry logEntry : logEntries) {
            handleLogEntry(logEntry);
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
            addLogHandler(logHandler);
        }
        return this;
    }

    @Override
    public synchronized ILogger addLogHandlers(Iterable<ILogHandler> logHandlers) {

        for (ILogHandler logHandler : logHandlers) {
            addLogHandler(logHandler);
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
            removeLogHandler(logHandler);
        }
        return this;
    }

    @Override
    public synchronized ILogger removeLogHandlers(Iterable<ILogHandler> logHandlers) {

        for (ILogHandler logHandler : logHandlers) {
            removeLogHandler(logHandler);
        }
        return this;
    }

    @Override
    public Iterable<ILogHandler> getLogHandlers() {
        return new ArrayList<>(this.handlers);
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

        this.logThread.interrupt();
        this.logThread.join();
        this.handlers.clear();
    }


    private void handleLogEntry(LogEntry logEntry) {
        if (logEntry != null && (level == -1 || logEntry.getLogLevel().getLevel() <= level)) {
            if (logEntry.getLogLevel().isAsync()) {
                entries.offer(new LogHandlerRunnable(logEntry));
            } else {
                new LogHandlerRunnable(logEntry).call();
            }
        }
    }

    public class LogHandlerRunnable implements Callable<Void> {

        private final LogEntry logEntry;

        public LogHandlerRunnable(LogEntry logEntry) {
            this.logEntry = logEntry;
        }

        @Override
        public Void call() {

            for (ILogHandler iLogHandler : handlers) {
                try {
                    iLogHandler.handle(logEntry);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            return null;
        }
    }

}