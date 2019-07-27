package de.dytanic.cloudnet.common.logging;

/**
 * The logger is designed to handle the application and has all the basic capabilities to provide easy-to-use logging
 * A logger can optionally process messages asynchronously. However, it is not an obligation for the implementation
 */
public interface ILogger extends ILogHandlerProvider<ILogger>, ILevelable, AutoCloseable {

    /**
     * Set the LogLevel notification level.
     * If the level higher or same than the incoming LogLevel.level. The LogEntry
     * is allowed to handle
     *
     * @param level the level, that should set
     * @see LogLevel
     */
    void setLevel(int level);

    /**
     * Allows to post one LogEntry object into the logger, which invokes the LogHandlers for this
     * LogEntry instance
     *
     * @param logEntry the entry, that should be handle
     * @return the current logger instance
     */
    ILogger log(LogEntry logEntry);

    /**
     * Allows to post zero or more LogEntries into the logger, which invokes the LogHandlers for this
     * LogEntry instances
     *
     * @param logEntries the entries, that should be handle
     * @return the current logger instance
     */
    ILogger log(LogEntry... logEntries);

    /**
     * Indicates, that the implementation of the logger has support for asynchronously log handle
     *
     * @return true when the implementation has the support or false if the class doesn't has any async features
     */
    boolean hasAsyncSupport();


    /**
     * A shortcut method from setLevel(level.getLevel()) to setLevel(level)
     *
     * @param level the LogLevel instance, from that the level integer value should get
     */
    default void setLevel(LogLevel level) {
        if (level == null) return;

        setLevel(level.getLevel());
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     */
    default ILogger log(LogLevel level, String message) {
        return log(level, new String[]{message});
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see ILogHandler
     */
    default ILogger log(LogLevel level, String... messages) {
        return log(level, messages, null);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see ILogHandler
     */
    default ILogger log(LogLevel level, Class<?> clazz, String message) {
        return log(level, clazz, new String[]{message});
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see ILogHandler
     */
    default ILogger log(LogLevel level, Class<?> clazz, String... messages) {
        return log(level, clazz, messages, null);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see ILogHandler
     */
    default ILogger log(LogLevel level, String message, Throwable throwable) {
        return log(level, new String[]{message}, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see ILogHandler
     */
    default ILogger log(LogLevel level, String[] messages, Throwable throwable) {
        return log(level, Thread.currentThread().getContextClassLoader().getClass(), messages, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see ILogHandler
     */
    default ILogger log(LogLevel level, Class<?> clazz, String message, Throwable throwable) {
        return log(level, clazz, new String[]{message}, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see ILogHandler
     */
    default ILogger log(LogLevel level, Class<?> clazz, String[] messages, Throwable throwable) {
        return log(new LogEntry(System.currentTimeMillis(), clazz, messages, level, throwable, Thread.currentThread()));
    }


    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel INFO
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger info(String message) {
        return log(LogLevel.INFO, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel INFO
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger info(String... messages) {
        return log(LogLevel.INFO, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel INFO
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger info(String message, Class<?> clazz) {
        return log(LogLevel.INFO, clazz, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel INFO
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger info(String[] messages, Class<?> clazz) {
        return log(LogLevel.INFO, clazz, messages);
    }


    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel WARNING
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger warning(String message) {
        return log(LogLevel.WARNING, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel WARNING
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger warning(String... messages) {
        return log(LogLevel.WARNING, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel WARNING
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger warning(String message, Class<?> clazz) {
        return log(LogLevel.WARNING, clazz, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel WARNING
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger warning(String[] messages, Class<?> clazz) {
        return log(LogLevel.WARNING, clazz, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel WARNING
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger warning(String message, Throwable throwable) {
        return log(LogLevel.WARNING, message, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel WARNING
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger warning(String[] messages, Throwable throwable) {
        return log(LogLevel.WARNING, messages, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel WARNING
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger warning(String message, Class<?> clazz, Throwable throwable) {
        return log(LogLevel.WARNING, clazz, message, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel WARNING
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger warning(String[] messages, Class<?> clazz, Throwable throwable) {
        return log(LogLevel.WARNING, clazz, messages, throwable);
    }


    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel FATAL
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger fatal(String message) {
        return log(LogLevel.FATAL, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel FATAL
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger fatal(String... messages) {
        return log(LogLevel.FATAL, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel FATAL
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger fatal(String message, Class<?> clazz) {
        return log(LogLevel.FATAL, clazz, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel FATAL
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger fatal(String[] messages, Class<?> clazz) {
        return log(LogLevel.FATAL, clazz, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel FATAL
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger fatal(String message, Throwable throwable) {
        return log(LogLevel.FATAL, message, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel FATAL
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger fatal(String[] messages, Throwable throwable) {
        return log(LogLevel.FATAL, messages, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel FATAL
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger fatal(String message, Class<?> clazz, Throwable throwable) {
        return log(LogLevel.FATAL, clazz, message, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel FATAL
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger fatal(String[] messages, Class<?> clazz, Throwable throwable) {
        return log(LogLevel.FATAL, clazz, messages, throwable);
    }


    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel ERROR
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger error(String message) {
        return log(LogLevel.ERROR, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel ERROR
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger error(String... messages) {
        return log(LogLevel.ERROR, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel ERROR
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger error(String message, Class<?> clazz) {
        return log(LogLevel.ERROR, clazz, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel ERROR
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger error(String[] messages, Class<?> clazz) {
        return log(LogLevel.ERROR, clazz, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel ERROR
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger error(String message, Throwable throwable) {
        return log(LogLevel.ERROR, message, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel ERROR
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger error(String[] messages, Throwable throwable) {
        return log(LogLevel.ERROR, messages, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel ERROR
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger error(String message, Class<?> clazz, Throwable throwable) {
        return log(LogLevel.ERROR, clazz, message, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel ERROR
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger error(String[] messages, Class<?> clazz, Throwable throwable) {
        return log(LogLevel.ERROR, clazz, messages, throwable);
    }


    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel DEBUG
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger debug(String message) {
        return log(LogLevel.DEBUG, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel DEBUG
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger debug(String... messages) {
        return log(LogLevel.DEBUG, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel DEBUG
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger debug(String message, Class<?> clazz) {
        return log(LogLevel.DEBUG, clazz, message);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel DEBUG
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger debug(String[] messages, Class<?> clazz) {
        return log(LogLevel.DEBUG, clazz, messages);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel DEBUG
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger debug(String message, Throwable throwable) {
        return log(LogLevel.DEBUG, message, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel DEBUG
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger debug(String[] messages, Throwable throwable) {
        return log(LogLevel.DEBUG, messages, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel DEBUG
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger debug(String message, Class<?> clazz, Throwable throwable) {
        return log(LogLevel.DEBUG, clazz, message, throwable);
    }

    /**
     * An wrapper method for the last base method log(LogEntry)
     * It has the default LogLevel DEBUG
     * <p>
     * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
     *
     * @see LogEntry
     * @see LogLevel
     * @see ILogHandler
     */
    default ILogger debug(String[] messages, Class<?> clazz, Throwable throwable) {
        return log(LogLevel.DEBUG, clazz, messages, throwable);
    }
}