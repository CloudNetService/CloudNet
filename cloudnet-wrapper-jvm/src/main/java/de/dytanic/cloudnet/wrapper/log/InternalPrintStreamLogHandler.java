package de.dytanic.cloudnet.wrapper.log;

import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.common.logging.LogLevel;

import java.io.PrintStream;

/**
 * A redirect log handler, to the origin output and error stream.
 * The LogOutputStream will replace the System.out and System.err stream after this initialization by the Wrapper
 */
public final class InternalPrintStreamLogHandler extends AbstractLogHandler {

    private final PrintStream outputStream, errorStream;

    public InternalPrintStreamLogHandler(PrintStream outputStream, PrintStream errorStream) {
        this.outputStream = outputStream;
        this.errorStream = errorStream;
    }

    @Override
    public void handle(LogEntry logEntry) {
        if (logEntry.getLogLevel().equals(LogLevel.ERROR) || logEntry.getLogLevel().equals(LogLevel.WARNING)) {
            this.errorStream.print(getFormatter().format(logEntry));
        } else {
            this.outputStream.print(getFormatter().format(logEntry));
        }
    }

    @Override
    public void close() {

    }
}