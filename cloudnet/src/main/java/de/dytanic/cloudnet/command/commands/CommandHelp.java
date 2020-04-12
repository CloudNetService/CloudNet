package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.common.language.LanguageManager;

import java.util.ArrayList;
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
                boolean light = false;
                for (CommandInfo commandInfo : getCloudNet().getCommandMap().getCommandInfos()) {
                    StringBuilder builder = new StringBuilder("Aliases: " + Arrays.toString(commandInfo.getNames()));
                    if (commandInfo.getPermission() != null) {
                        builder.append(" | Permission: ").append(commandInfo.getPermission());
                    }
                    if (commandInfo.getDescription() != null) {
                        builder.append(" - ").append(commandInfo.getDescription());
                    }
                    sender.sendMessage((light ? "&7" : "&8") + builder.toString());
                    light = !light;
                }
                sender.sendMessage(LanguageManager.getMessage("command-help-info"));

                break;
            case 1:

                if (getCloudNet().getCommandMap().getCommandNames().contains(args[0].toLowerCase())) {
                    Command commandInfo = getCloudNet().getCommandMap().getCommand(args[0].toLowerCase());

                    if (commandInfo != null) {
                        sender.sendMessage(" ", "Aliases: " + Arrays.toString(commandInfo.getNames()));
                        if (commandInfo.getDescription() != null) {
                            sender.sendMessage("Description: " + commandInfo.getDescription());
                        }
                        if (commandInfo.getUsage() != null) {
                            String[] usage = ("Usage: " + commandInfo.getUsage()).split("\n");
                            for (String line : usage) {
                                sender.sendMessage(line);
                            }
                        }
                    }
                }
                break;
        }

    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        Collection<String> x = new ArrayList<>();

        for (CommandInfo commandInfo : getCloudNet().getCommandMap().getCommandInfos()) {
            x.addAll(Arrays.asList(commandInfo.getNames()));
        }

        return x;
    }
}