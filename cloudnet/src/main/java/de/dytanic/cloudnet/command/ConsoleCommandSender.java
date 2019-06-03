package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import lombok.AllArgsConstructor;

/**
 * The ConsoleCommandSender represents the console of the application. The console has
 * all needed permissions.
 */
@AllArgsConstructor
public final class ConsoleCommandSender implements ICommandSender {

    private static final LogLevel CONSOLE_LEVEL = new LogLevel("console", "CONSOLE", 6, true);

    private final ILogger logger;

    /**
     * The console name is the first codename from CloudNet 3.0: "Tsunami"
     */
    @Override
    public String getName() {
        return "Tsunami";
    }

    @Override
    public void sendMessage(String message) {
        this.logger.log(CONSOLE_LEVEL, message);
    }

    @Override
    public void sendMessage(String... messages) {
        Validate.checkNotNull(messages);

        for (String message : messages)
            this.sendMessage(message);
    }

    /**
     * The console as always the permission for by every request
     */
    @Override
    public boolean hasPermission(String permission) {
        return true;
    }
}