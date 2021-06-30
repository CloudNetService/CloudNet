/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

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
