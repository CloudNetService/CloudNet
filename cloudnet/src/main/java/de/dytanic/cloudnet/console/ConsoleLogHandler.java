package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import org.jetbrains.annotations.NotNull;

public final class ConsoleLogHandler extends AbstractLogHandler {

    private final IConsole console;

    public ConsoleLogHandler(IConsole console) {
        this.console = console;
    }

    @Override
    public void handle(@NotNull LogEntry logEntry) {
        this.console.writeLine(this.getFormatter().format(logEntry));
    }

    public @NotNull IConsole getConsole() {
        return this.console;
    }
}