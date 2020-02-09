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
import java.util.stream.Collectors;

public class JLine3Completer implements Completer {

    private JLine3Console console;

    public JLine3Completer(JLine3Console console) {
        this.console = console;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        candidates.clear();

        String buffer = line.line();
        String[] args = buffer.split(" ");

        String testString = args.length <= 1 || buffer.endsWith(" ") ? "" : args[args.length - 1].toLowerCase().trim();

        if (buffer.lastIndexOf(' ') == -1) {
            args = new String[0];
        } else {
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        Properties parsed = Properties.parseLine(args);

        Collection<String> temp = new ArrayList<>();
        for (ITabCompleter iTabCompleter : this.console.getTabCompletionHandler()) {
            Collection<String> completerResponses = iTabCompleter.complete(buffer, args, parsed);
            if (completerResponses != null && !completerResponses.isEmpty()) {
                temp.addAll(completerResponses);
            }
        }

        if (temp.isEmpty()) {
            return;
        }

        List<String> sortedResponses = temp.stream()
                .filter(response -> response != null && (testString.isEmpty() || response.toLowerCase().startsWith(testString)))
                .sorted()
                .collect(Collectors.toList());
        candidates.addAll(sortedResponses.stream().map(Candidate::new).collect(Collectors.toList()));
    }
}
