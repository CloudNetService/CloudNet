package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.console.JLine2Console;

import java.io.IOException;

public final class CommandClear extends CommandDefault {

    public CommandClear() {
        super("clear");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        try {
            ((JLine2Console) getCloudNet().getConsole()).getConsoleReader().clearScreen();
        } catch (IOException ignored) {
        }
    }
}