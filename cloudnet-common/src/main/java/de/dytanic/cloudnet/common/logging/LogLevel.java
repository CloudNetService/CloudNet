package de.dytanic.cloudnet.common.logging;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * The LogLevel indicates how relevant and essential information is to be output.
 * All LogEntry instances has a LogLevel, which describe his relevant
 * <p>
 * An LogLevel configure the async log printing feature for the logger, if the logger implementation support
 * asynchronously ILogHandler invocation
 *
 * @see LogEntry
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class LogLevel implements ILevelable {

    /**
     * The INFO level is the basic level, for normal messages like "The sun is shining"
     * <p>
     * Allows asynchronously ILogHandler invocation
     */
    public static final LogLevel INFO = new LogLevel("information", "INFO", 0, true);

    /**
     * The IMPORTANT level is for important messages like "Oh, a bird has hit me!"
     * <p>
     * Allows asynchronously ILogHandler invocation
     */
    public static final LogLevel IMPORTANT = new LogLevel("important", "IMPORTANT", 1, true);

    /**
     * The COMMAND level is for the console messages by handling a command from the command input line
     * <p>
     * Disallows asynchronously ILogHandler invocation
     */
    public static final LogLevel COMMAND = new LogLevel("command", "COMMAND", 1, false);

    /**
     * The WARNING level is for important messages like "WARNING! Your program may crash"
     * <p>
     * Allows asynchronously ILogHandler invocation
     */
    public static final LogLevel WARNING = new LogLevel("warning", "WARNING", 125, true);

    /**
     * The ERROR level is for important messages like a StackTrace or a error message
     * <p>
     * Allows asynchronously ILogHandler invocation
     */
    public static final LogLevel ERROR = new LogLevel("error", "ERROR", 126, true);

    /**
     * The FATAL level is for crashing messages or messages that are so important, that can't describe with ERROR
     * <p>
     * Disallows asynchronously ILogHandler invocation
     */
    public static final LogLevel FATAL = new LogLevel("fatal", "FATAL", 127, false);

    /**
     * The DEBUG level is for crashing messages or messages that are so important, that can
     * <p>
     * Disallows asynchronously ILogHandler invocation
     */
    public static final LogLevel DEBUG = new LogLevel("debug", "DEBUG", 128, false);

    /**
     * A wildcard that all messages can be handle from the Logger, if the logger invokes the
     * setLevel(LogLevel.ALL) method.
     */
    public static final LogLevel ALL = new LogLevel("all", "ALL", Integer.MAX_VALUE, true);

    /**
     * The level name in lower and upper case form.
     * It is important fort the format of the end messages
     */
    protected String lowerName, upperName;

    /**
     * Defines the current level as int value
     */
    protected int level;

    /**
     * Defines the permission, to execute the LogEntries on this Level asynchronously or not.
     */
    protected boolean async;
}