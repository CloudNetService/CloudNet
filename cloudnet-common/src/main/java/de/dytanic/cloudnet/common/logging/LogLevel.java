package de.dytanic.cloudnet.common.logging;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * The LogLevel indicates how relevant and essential information is to be output.
 * All LogEntry instances has a LogLevel, which describe his relevant
 * <p>
 * An LogLevel configure the async log printing feature for the logger, if the logger implementation support
 * asynchronously ILogHandler invocation
 *
 * @see LogEntry
 */
@ToString
@EqualsAndHashCode
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
    public static final LogLevel WARNING = new LogLevel("warning", "WARNING", 125, true, true);

    /**
     * The ERROR level is for important messages like a StackTrace or a error message
     * <p>
     * Allows asynchronously ILogHandler invocation
     */
    public static final LogLevel ERROR = new LogLevel("error", "ERROR", 126, true, true);

    /**
     * The FATAL level is for crashing messages or messages that are so important, that can't describe with ERROR
     * <p>
     * Disallows asynchronously ILogHandler invocation
     */
    public static final LogLevel FATAL = new LogLevel("fatal", "FATAL", 127, true, false);

    /**
     * The EXTENDED level is for more precise information messages that can be disabled
     * <p>
     * Disallows asynchronously ILogHandler invocation
     */
    public static final LogLevel EXTENDED = new LogLevel("extended", "EXTENDED", 128, false);

    /**
     * The DEBUG level is for all debugging messages, that are not important for CloudNet's runtime but may be useful
     * if an error occurred
     * <p>
     * Disallows asynchronously ILogHandler invocation
     */
    public static final LogLevel DEBUG = new LogLevel("debug", "DEBUG", 129, false);

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

    protected boolean colorized;

    /**
     * Defines the permission, to execute the LogEntries on this Level asynchronously or not.
     */
    protected boolean async;

    public LogLevel(String lowerName, String upperName, int level, boolean async) {
        this(lowerName, upperName, level, false, async);
    }

    public LogLevel(String lowerName, String upperName, int level, boolean colorized, boolean async) {
        this.lowerName = lowerName;
        this.upperName = upperName;
        this.level = level;
        this.colorized = colorized;
        this.async = async;
    }

    public String getLowerName() {
        return lowerName;
    }

    public String getUpperName() {
        return upperName;
    }

    @Override
    public int getLevel() {
        return level;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isColorized() {
        return colorized;
    }

    public static Optional<LogLevel> getDefaultLogLevel(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable((LogLevel) LogLevel.class.getField(name).get(null));
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            return Optional.empty();
        }
    }

}