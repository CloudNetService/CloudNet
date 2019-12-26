package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.logging.LogLevel;

public class CommandDebug extends CommandDefault {
    public CommandDebug() {
        super("debug");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (CloudNet.getInstance().getLogger().getLevel() == LogLevel.DEBUG.getLevel()) {
            CloudNet.getInstance().setGlobalLogLevel(CloudNet.getInstance().getDefaultLogLevel());
        } else {
            CloudNet.getInstance().setGlobalLogLevel(LogLevel.DEBUG);
        }
    }
}
