package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;

public final class CommandClear extends CommandDefault {

    public CommandClear() {
        super("clear");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        getCloudNet().getConsole().clearScreenAndCache();
    }
}