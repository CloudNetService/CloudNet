package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.common.Validate;

import java.util.Arrays;
import java.util.Collection;

/**
 * The driverCommandSender
 */
public final class DriverCommandSender implements ICommandSender {

    private final Collection<String> messages;

    public DriverCommandSender(Collection<String> messages) {
        this.messages = messages;
    }

    @Override
    public String getName() {
        return "DriverCommandSender";
    }

    @Override
    public void sendMessage(String message) {
        Validate.checkNotNull(message);

        this.messages.add(message);
    }

    @Override
    public void sendMessage(String... messages) {
        Validate.checkNotNull(messages);

        this.messages.addAll(Arrays.asList(messages));
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }
}