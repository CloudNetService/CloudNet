package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class JLine3Completer implements Completer {

    private final JLine3Console console;

    public JLine3Completer(JLine3Console console) {
        this.console = console;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String buffer = line.line();
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
            responses
                    .stream()
                    .filter(response -> response != null && (testString.isEmpty() || response.toLowerCase().startsWith(testString)))
                    .sorted()
                    .map(Candidate::new)
                    .forEach(candidates::add);
        }
    }
}
