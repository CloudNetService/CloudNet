package de.dytanic.cloudnet.command;

public class CommandExecutionException extends RuntimeException {
    public CommandExecutionException(String command, Throwable cause) {
        super("An error occurred while attempting to perform command \"" + command + "\"", cause);
    }
}
