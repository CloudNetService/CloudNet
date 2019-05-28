package de.dytanic.cloudnet.command.jline2;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandMap;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Iterables;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import jline.console.completer.Completer;
import lombok.AllArgsConstructor;

/**
 * JLine2 implementation of the completer for the JLine console
 */
@AllArgsConstructor
public final class JLine2CommandCompleter implements Completer {

  private final ICommandMap commandMap;

  @Override
  public int complete(String buffer, int cursor,
      List<CharSequence> candidates) {
    List<String> responses = Iterables.newArrayList();

    if (buffer.isEmpty() || buffer.indexOf(' ') == -1) {
      responses.addAll(Iterables
          .filter(commandMap.getCommandNames(), new Predicate<String>() {

            @Override
            public boolean test(String s) {
              return s != null && s.toLowerCase()
                  .startsWith(buffer.toLowerCase());
            }
          }));
    } else {
      Command command = commandMap.getCommandFromLine(buffer);

      if (command instanceof ITabCompleter) {
        String[] args = buffer.split(" ");
        String testString = args[args.length - 1];
        args = buffer.replaceFirst(args[0] + " ", "").split(" ");

        responses.addAll(Iterables.filter(((ITabCompleter) command)
                .complete(buffer, args, Properties.parseLine(args)),
            new Predicate<String>() {

              @Override
              public boolean test(String s) {
                return s != null && (testString.isEmpty() || s.toLowerCase()
                    .startsWith(testString.toLowerCase()));
              }
            }));
      }
    }

    Collections.sort(responses);

    candidates.addAll(responses);
    int lastSpace = buffer.lastIndexOf(' ');

    return (lastSpace == -1) ? cursor - buffer.length()
        : cursor - (buffer.length() - lastSpace - 1);
  }
}