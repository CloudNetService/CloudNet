package de.dytanic.cloudnet.common.logging;

/**
 * The Formatter is a simply abstract way to format a LogEntry to
 * a easy, formatted, readable message.
 */
public interface IFormatter {

    /**
     * Formats a logEntry into a readable text
     *
     * @param logEntry the log item which should be format
     * @return the new formatted string
     */
    String format(LogEntry logEntry);

}