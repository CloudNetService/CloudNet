package de.dytanic.cloudnet.command.sub;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.LanguageManager;

import java.util.*;
import java.util.stream.Collectors;

public class SubCommandHandler extends Command implements ITabCompleter {

    private Collection<SubCommand> subCommands;

    public SubCommandHandler(Collection<SubCommand> subCommands, String... names) {
        super(names);
        this.subCommands = subCommands;
    }

    public SubCommandHandler(String[] names, String permission, Collection<SubCommand> subCommands) {
        super(names, permission);
        this.subCommands = subCommands;
    }

    public SubCommandHandler(String[] names, String permission, String description, Collection<SubCommand> subCommands) {
        super(names, permission, description);
        this.subCommands = subCommands;
    }

    public SubCommandHandler(String[] names, String permission, String description, String usage, String prefix, Collection<SubCommand> subCommands) {
        super(names, permission, description, usage, prefix);
        this.subCommands = subCommands;
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        Optional<String> optionalInvalidMessage = this.subCommands.stream()
                .map(subCommand -> subCommand.getInvalidArgumentMessage(args))
                .filter(Objects::nonNull)
                .filter(pair -> pair.getSecond() == 0)
                // all static values must match
                // (e.g. "tasks task <name> set minServiceCount <value>" -> "task", "set" and "minServiceCount" must match,
                //   otherwise this wasn't the sub command printed by the user -> e.g. "tasks task Lobby set maintenance 1")
                // now, we find the pair with the most matching arguments (actually we find the pair with the least non-matching arguments, but in the end it's the same):
                .min(Comparator.comparingInt(Pair::getSecond))
                .map(Pair::getFirst);

        Optional<Pair<SubCommand, Object[]>> optionalSubCommand = this.subCommands.stream()
                .map(subCommand -> new Pair<>(subCommand, subCommand.parseArgs(args)))
                .filter(pair -> pair.getSecond() != null)
                .findFirst();

        if (optionalInvalidMessage.isPresent() && !optionalSubCommand.isPresent()) {
            sender.sendMessage(optionalInvalidMessage.get());
            return;
        }

        if (!optionalSubCommand.isPresent()) {
            this.sendHelp(sender);
            return;
        }


        Pair<SubCommand, Object[]> subCommandPair = optionalSubCommand.get();

        SubCommand subCommand = subCommandPair.getFirst();
        Object[] parsedArgs = subCommandPair.getSecond();

        if (subCommand.isOnlyConsole() && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(LanguageManager.getMessage("command-sub-only-console"));
            return;
        }

        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(LanguageManager.getMessage("command-sub-no-permission"));
            return;
        }

        subCommand.execute(sender, command, parsedArgs, commandLine, properties, new HashMap<>());
    }

    protected void sendHelp(ICommandSender sender) {
        Collection<String> messages = new ArrayList<>();
        for (SubCommand subCommand : this.subCommands) {
            String message = super.getNames()[0] + " " + subCommand.getRequiredArgsAsString();

            if (subCommand.getPermission() != null) {
                message += " | " + subCommand.getPermission();
            }

            if (subCommand.getDescription() != null) {
                message += " | " + subCommand.getDescription();
            }
            messages.add(message);
        }
        sender.sendMessage(messages.toArray(new String[0]));
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        return this.subCommands.stream()
                .map(subCommand -> subCommand.getNextPossibleArgumentAnswers(args))
                .filter(Objects::nonNull)
                .filter(responses -> !responses.isEmpty())
                .flatMap(Collection::parallelStream)
                .collect(Collectors.toList());
    }
}
