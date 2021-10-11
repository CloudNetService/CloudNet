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

import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.source.ConsoleCommandSource;
import java.util.Collection;
import java.util.List;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class JLine3Completer implements Completer {

  private final JLine3Console console;
  private final CommandProvider commandProvider;

  public JLine3Completer(JLine3Console console, CommandProvider commandProvider) {
    this.console = console;
    this.commandProvider = commandProvider;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    Collection<String> responses = this.commandProvider.suggest(ConsoleCommandSource.INSTANCE, line.line());
    if (!responses.isEmpty()) {
      responses
        .stream()
        .map(Candidate::new)
        .forEach(candidates::add);
    }
  }
}
