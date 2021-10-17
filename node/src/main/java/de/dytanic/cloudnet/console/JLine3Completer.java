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

import de.dytanic.cloudnet.console.handler.Toggleable;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

final class JLine3Completer implements Completer {

  private final JLine3Console console;

  public JLine3Completer(@NotNull JLine3Console console) {
    this.console = console;
  }

  @Override
  public void complete(LineReader reader, @NotNull ParsedLine line, @NotNull List<Candidate> candidates) {
    this.console.getTabCompleteHandlers().values().stream()
      .filter(Toggleable::isEnabled)
      .flatMap(handler -> handler.completeInput(line.line()).stream())
      .filter(Objects::nonNull)
      .map(Candidate::new)
      .forEach(candidates::add);
  }
}
