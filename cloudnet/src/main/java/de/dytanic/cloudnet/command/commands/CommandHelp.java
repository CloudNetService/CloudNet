package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.command.CommandInfo;

import java.util.Arrays;
import java.util.Collection;

public final class CommandHelp extends CommandDefault implements ITabCompleter {

    public CommandHelp() {
        super("help", "ask");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        switch (args.length) {
            case 0:

                for (CommandInfo commandInfo : getCloudNet().getCommandMap().getCommandInfos()) {
                    sender.sendMessage("Aliases: " + Arrays.toString(commandInfo.getNames()) + " - " + commandInfo.getDescription());
                }

                break;
            case 1:

                if (getCloudNet().getCommandMap().getCommandNames().contains(args[0].toLowerCase())) {
                    Command commandInfo = getCloudNet().getCommandMap().getCommand(args[0].toLowerCase());

                    if (commandInfo != null) {
                        sender.sendMessage(
                                " ",
                                "Aliases: " + Arrays.toString(commandInfo.getNames()),
                                "Description: " + commandInfo.getDescription(),
                                "Usage: " + commandInfo.getUsage()
                        );
                    }
                }
                break;
        }

    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        Collection<String> x = Iterables.newArrayList();

        for (CommandInfo commandInfo : getCloudNet().getCommandMap().getCommandInfos()) {
            x.addAll(Arrays.asList(commandInfo.getNames()));
        }

        return x;
    }
}