package de.dytanic.cloudnet.command;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;

/**
 * The ConsoleCommandSender represents the console of the application. The console has
 * all needed permissions.
 */
public final class ConsoleCommandSender implements ICommandSender {

    private final ILogger logger;

    public ConsoleCommandSender(ILogger logger) {
        this.logger = logger;
    }

    /**
     * The console name is the codename of the current CloudNet version
     */
    @Override
    public String getName() {
        return "Earthquake";
    }

    @Override
    public void sendMessage(String message) {
        this.logger.log(LogLevel.COMMAND, message);
    }

    @Override
    public void sendMessage(String... messages) {
        Preconditions.checkNotNull(messages);

        for (String message : messages) {
            this.sendMessage(message);
        }
    }

    /**
     * The console as always the permission for by every request
     */
    @Override
    public boolean hasPermission(String permission) {
        return true;
    }
}
