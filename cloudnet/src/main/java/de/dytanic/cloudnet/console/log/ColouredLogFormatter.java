package de.dytanic.cloudnet.console.log;

import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.console.ConsoleColor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public final class ColouredLogFormatter implements IFormatter {

    private final DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss.SSS");

    @Override
    public String format(LogEntry logEntry)
    {
        StringBuilder builder = new StringBuilder();
        if (logEntry.getThrowable() != null)
        {
            StringWriter writer = new StringWriter();
            logEntry.getThrowable().printStackTrace(new PrintWriter(writer));
            builder.append(writer).append(System.lineSeparator());
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (String message : logEntry.getMessages())
            if (message != null)
                stringBuilder
                    .append(ConsoleColor.DARK_GRAY)
                    .append("[")
                    .append(ConsoleColor.WHITE)
                    .append(dateFormat.format(logEntry.getTimeStamp()))
                    .append(ConsoleColor.DARK_GRAY)
                    .append("] ")
                    .append(logEntry.getLogLevel().getLevel() < LogLevel.WARNING.getLevel() ? ConsoleColor.GRAY : ConsoleColor.RED)
                    .append(logEntry.getLogLevel().getUpperName())
                    .append(ConsoleColor.DARK_GRAY)
                    .append(": ")
                    .append(logEntry.getLogLevel().getLevel() < LogLevel.WARNING.getLevel() ? ConsoleColor.DEFAULT : ConsoleColor.YELLOW)
                    .append(message)
                    .append(System.lineSeparator());

        return stringBuilder.append(builder).toString();
    }
}