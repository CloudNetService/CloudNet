package de.dytanic.cloudnet.command.jline2;

import de.dytanic.cloudnet.command.ICommandMap;
import jline.console.completer.Completer;

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
        List<String> responses = this.commandMap.tabCompleteCommand(buffer);
        Collections.sort(responses);
        candidates.addAll(responses);

        int lastSpace = buffer.lastIndexOf(' ');

        return (lastSpace == -1) ? cursor - buffer.length() : cursor - (buffer.length() - lastSpace - 1);
    }
}