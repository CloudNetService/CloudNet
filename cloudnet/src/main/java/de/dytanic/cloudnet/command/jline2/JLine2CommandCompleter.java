package de.dytanic.cloudnet.command.jline2;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandMap;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Iterables;
import jline.console.completer.Completer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JLine2 implementation of the completer for the JLine console
 */
public final class JLine2CommandCompleter implements Completer {

    private final ICommandMap commandMap;

    public JLine2CommandCompleter(ICommandMap commandMap) {
        this.commandMap = commandMap;
    }

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        List<String> responses = Iterables.newArrayList();

        if (buffer.isEmpty() || buffer.indexOf(' ') == -1) {
            responses.addAll(Iterables.filter(commandMap.getCommandNames(), s -> s != null && s.toLowerCase().startsWith(buffer.toLowerCase())));
        } else {
            Command command = commandMap.getCommandFromLine(buffer);

            if (command instanceof ITabCompleter) {
                String[] args = buffer.split(" ");
                String testString = args.length <= 1 || buffer.endsWith(" ") ? "" : args[args.length - 1].toLowerCase().trim();
                if (buffer.endsWith(" ")) {
                    args = Arrays.copyOfRange(args, 1, args.length + 1);
                    args[args.length - 1] = "";
                } else {
                    args = Arrays.copyOfRange(args, 1, args.length);
                }

                responses.addAll(Iterables.filter(((ITabCompleter) command).complete(buffer, args, Properties.parseLine(args)), s -> s != null && (testString.isEmpty() || s.toLowerCase().startsWith(testString))));
            }
        }

        Collections.sort(responses);

        candidates.addAll(responses);
        int lastSpace = buffer.lastIndexOf(' ');

        return (lastSpace == -1) ? cursor - buffer.length() : cursor - (buffer.length() - lastSpace - 1);
    }
}