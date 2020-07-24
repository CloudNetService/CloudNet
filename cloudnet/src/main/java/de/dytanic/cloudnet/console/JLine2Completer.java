package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import jline.console.completer.Completer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JLine2Completer implements Completer {

    private final JLine2Console console;

    public JLine2Completer(JLine2Console console) {
        this.console = console;
    }

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        String[] args = buffer.split(" ");
        String testString = args.length <= 1 || buffer.endsWith(" ") ? "" : args[args.length - 1].toLowerCase().trim();
        if (args.length > 1) {
            if (buffer.endsWith(" ")) {
                args = Arrays.copyOfRange(args, 1, args.length + 1);
                args[args.length - 1] = "";
            } else {
                args = Arrays.copyOfRange(args, 1, args.length);
            }
        }

        Collection<String> responses = new ArrayList<>();
        for (ITabCompleter completer : this.console.getTabCompletionHandler()) {
            Collection<String> completerResponses = completer.complete(buffer, args, Properties.parseLine(args));
            if (completerResponses != null && !completerResponses.isEmpty()) {
                responses.addAll(completerResponses);
            }
        }

        if (!responses.isEmpty()) {
            List<String> sortedResponses = responses.stream().filter(response -> response != null && (testString.isEmpty() || response.toLowerCase().startsWith(testString))).sorted().collect(Collectors.toList());
            candidates.addAll(sortedResponses);
        }

        int lastSpace = buffer.lastIndexOf(' ');

        return (lastSpace == -1) ? cursor - buffer.length() : cursor - (buffer.length() - lastSpace - 1);
    }

}
