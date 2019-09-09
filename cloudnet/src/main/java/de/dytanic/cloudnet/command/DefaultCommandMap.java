package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.command.CommandInfo;

import java.util.Collection;
import java.util.Map;

public final class DefaultCommandMap implements ICommandMap {

    private final Map<String, Command> registeredCommands = Maps.newConcurrentHashMap();

    @Override
    public void registerCommand(Command command) {
        if (command != null && command.isValid()) {
            for (String name : command.getNames()) {
                this.registeredCommands.put(name.toLowerCase(), command);

                if (command.getPrefix() != null && !command.getPrefix().isEmpty()) {
                    this.registeredCommands.put(command.getPrefix().toLowerCase() + ":" + name.toLowerCase(), command);
                }
            }
        }
    }

    @Override
    public void unregisterCommand(String command) {
        this.registeredCommands.remove(command);
    }

    @Override
    public void unregisterCommand(Class<? extends Command> command) {
        Validate.checkNotNull(command);

        for (Command commandEntry : this.registeredCommands.values()) {
            if (commandEntry.getClass().equals(command)) {
                for (String commandName : commandEntry.getNames()) {
                    this.registeredCommands.remove(commandName.toLowerCase());

                    if (commandEntry.getPrefix() != null && !commandEntry.getPrefix().isEmpty()) {
                        this.registeredCommands.remove(commandEntry.getPrefix().toLowerCase() + ":" + commandName.toLowerCase());
                    }
                }
            }
        }
    }

    @Override
    public void unregisterCommands(ClassLoader classLoader) {
        Validate.checkNotNull(classLoader);

        for (Command commandEntry : this.registeredCommands.values()) {
            if (commandEntry.getClass().getClassLoader().equals(classLoader)) {
                for (String commandName : commandEntry.getNames()) {
                    this.registeredCommands.remove(commandName.toLowerCase());

                    if (commandEntry.getPrefix() != null && !commandEntry.getPrefix().isEmpty()) {
                        this.registeredCommands.remove(commandEntry.getPrefix().toLowerCase() + ":" + commandName.toLowerCase());
                    }
                }
            }
        }
    }

    @Override
    public void unregisterCommands() {
        this.registeredCommands.clear();
    }

    @Override
    public Collection<CommandInfo> getCommandInfos() {
        Collection<Command> commands = Iterables.newArrayList();

        for (Command command : this.registeredCommands.values()) {
            if (!commands.contains(command)) {
                commands.add(command);
            }
        }

        return Iterables.map(commands, this::commandInfoFilter);
    }

    @Override
    public Command getCommand(String name) {
        if (name == null) {
            return null;
        }

        return this.registeredCommands.get(name.toLowerCase());
    }

    @Override
    public Command getCommandFromLine(String commandLine) {
        if (commandLine == null || commandLine.isEmpty()) {
            return null;
        }

        String[] a = commandLine.split(" ");
        return a.length >= 1 ? this.registeredCommands.get(a[0].toLowerCase()) : null;
    }

    @Override
    public Collection<String> getCommandNames() {
        return this.registeredCommands.keySet();
    }

    @Override
    public boolean dispatchCommand(ICommandSender commandSender, String commandLine) {
        if (commandSender == null || commandLine == null || commandLine.trim().isEmpty()) {
            return false;
        }

        boolean response = true;

        String[] commands = commandLine.split(" && ");

        for (String command : commands) {
            response = response && this.dispatchCommand0(commandSender, command);
        }

        return response;
    }

    public boolean dispatchCommand0(ICommandSender commandSender, String commandLine) {
        String[] args = commandLine.split(" ");

        if (!this.registeredCommands.containsKey(args[0].toLowerCase())) {
            return false;
        }

        Command command = this.registeredCommands.get(args[0].toLowerCase());
        String commandName = args[0].toLowerCase();

        if (command.getPermission() != null && !commandSender.hasPermission(command.getPermission())) {
            return false;
        }

        args = args.length > 1 ? commandLine.replaceFirst(args[0] + " ", "").split(" ") : new String[0];

        try {
            command.execute(commandSender, commandName, args, commandLine, Properties.parseLine(args));
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }


    private CommandInfo commandInfoFilter(Command command) {
        return command.getInfo();
    }

}