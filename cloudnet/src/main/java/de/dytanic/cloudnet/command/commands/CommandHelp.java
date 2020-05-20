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
                CommandInfo[] infos = this.getCloudNet().getCommandMap().getCommandInfos().toArray(new CommandInfo[0]);
                StringBuilder[] entries = new StringBuilder[infos.length];

                for (int i = 0; i < entries.length; i++) {
                    entries[i] = new StringBuilder();
                }

                for (int i = 0; i < infos.length; i++) {
                    entries[i].append("Aliases: ").append(Arrays.toString(infos[i].getNames()));
                }

                int maxLength = Arrays.stream(entries).mapToInt(StringBuilder::length).max().orElse(0);

                this.fill(maxLength, entries);

                for (int i = 0; i < infos.length; i++) {
                    if (infos[i].getPermission() != null) {
                        entries[i].append(" | Permission: ").append(infos[i].getPermission());
                    }
                }

                maxLength = Arrays.stream(entries).mapToInt(StringBuilder::length).max().orElse(0);

                this.fill(maxLength, entries);

                for (int i = 0; i < infos.length; i++) {
                    if (infos[i].getDescription() != null) {
                        entries[i].append(" - ").append(infos[i].getDescription());
                    }
                }

                for (StringBuilder entry : entries) {
                    sender.sendMessage(entry.toString());
                }
                sender.sendMessage(LanguageManager.getMessage("command-help-info"));

                break;
            case 1:

                if (this.getCloudNet().getCommandMap().getCommandNames().contains(args[0].toLowerCase())) {
                    Command commandInfo = this.getCloudNet().getCommandMap().getCommand(args[0].toLowerCase());

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

    private void fill(int maxLength, StringBuilder[] entries) {
        for (StringBuilder entry : entries) {
            int missing = maxLength - entry.length();
            for (int j = 0; j < missing; j++) {
                entry.append(' ');
            }
        }
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        Collection<String> x = new ArrayList<>();

        for (CommandInfo commandInfo : this.getCloudNet().getCommandMap().getCommandInfos()) {
            x.addAll(Arrays.asList(commandInfo.getNames()));
        }

        return x;
    }
}