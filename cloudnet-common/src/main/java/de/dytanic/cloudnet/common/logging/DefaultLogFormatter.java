package de.dytanic.cloudnet.common.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * The default log formatter, defines a fallback log format for all AbstractLogHandler implementations
 * <p>
 * The message format looks like:
 * <p>[15.02 23:32.56.456] INFO: Hello, world!</p>
 *
 * @see AbstractLogHandler
 */
public final class DefaultLogFormatter implements IFormatter {

    private final DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss.SSS");

    @Override
    public String format(LogEntry logEntry) {
        StringBuilder builder = new StringBuilder();
        LoggingUtils.printStackTraceToStringBuilder(builder, logEntry.getThrowable());

        StringBuilder stringBuilder = new StringBuilder();

        for (String message : logEntry.getMessages()) {
            if (message != null) {
                stringBuilder
                        .append("[")
                        .append(dateFormat.format(logEntry.getTimeStamp()))
                        .append("] ")
                        .append(logEntry.getLogLevel().getUpperName())
                        .append(": ")
                        .append(message)
                        .append(System.lineSeparator());
            }
        }


        stringBuilder.append(builder);

        return stringBuilder.toString();
    }
}