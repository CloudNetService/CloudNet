package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class ConsoleLogHandler extends AbstractLogHandler {

    private final IConsole console;

    @Override
    public void handle(LogEntry logEntry)
    {
        console.writeLine(getFormatter().format(logEntry));
    }

    @Override
    public void close() throws Exception
    {

    }
}