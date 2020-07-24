package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;

public final class ConsoleLogHandler extends AbstractLogHandler {

    private final IConsole console;

    public ConsoleLogHandler(IConsole console) {
        this.console = console;
    }

    @Override
    public void handle(LogEntry logEntry) {
        this.console.writeLine(this.getFormatter().format(logEntry));
    }

    @Override
    public void close() {

    }

    public IConsole getConsole() {
        return this.console;
    }
}