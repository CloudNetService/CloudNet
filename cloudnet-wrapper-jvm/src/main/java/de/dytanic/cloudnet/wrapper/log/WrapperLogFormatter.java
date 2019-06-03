package de.dytanic.cloudnet.wrapper.log;

import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.LogEntry;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class WrapperLogFormatter implements IFormatter {

    @Override
    public String format(LogEntry logEntry) {
        StringBuilder builder = new StringBuilder();
        if (logEntry.getThrowable() != null) {
            StringWriter writer = new StringWriter();
            logEntry.getThrowable().printStackTrace(new PrintWriter(writer));
            builder.append(writer).append(System.lineSeparator());
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (String line : logEntry.getMessages())
            if (line != null) stringBuilder.append(line);

        return stringBuilder.append(builder).toString();
    }
}