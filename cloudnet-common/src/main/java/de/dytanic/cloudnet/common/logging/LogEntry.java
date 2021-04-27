package de.dytanic.cloudnet.common.logging;

/**
 * Represents a full log record from the logger.
 * Important for the most loggers are the following information
 *
 * <ol>
 * <li>timestamp: for the time, from the log entry</li>
 * <li>messages: all messages in this record </li>
 * <li>thread: the Thread in that the record was created</li>
 * </ol>
 */
public class LogEntry {

    /**
     * The timestamp in that the log record was create in millis
     * from since 01.01.1970 00:00:00
     */
    protected final long timeStamp;

    /**
     * The class in that the logger should print if the class is defined
     * The class can be null
     */
    protected final Class<?> clazz;

    /**
     * All messages, which should print from the logger as array of messages to
     * execute more message as one in a entry
     * <p>
     * It's not allowed to set the array or the entries of that to null.
     * The log entry will be blocked
     */
    protected final String[] messages;

    /**
     * The LogLevel of this LogEntry.
     * The LogLevel must be not null, but it can be custom created
     */
    protected final LogLevel logLevel;

    /**
     * An optional Throwable instance, for error messages that are should interesting
     * for the log handlers
     */
    protected final Throwable throwable;

    /**
     * The Thread instance in that the LogEntry was created
     */
    protected final Thread thread;

    public LogEntry(long timeStamp, Class<?> clazz, String[] messages, LogLevel logLevel, Throwable throwable, Thread thread) {
        this.timeStamp = timeStamp;
        this.clazz = clazz;
        this.messages = messages;
        this.logLevel = logLevel;
        this.throwable = throwable;
        this.thread = thread;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public Class<?> getClazz() {
        return this.clazz;
    }

    public String[] getMessages() {
        return this.messages;
    }

    public LogLevel getLogLevel() {
        return this.logLevel;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }

    public Thread getThread() {
        return this.thread;
    }
}