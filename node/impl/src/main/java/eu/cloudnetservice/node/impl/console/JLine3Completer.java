/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.console;

import java.util.List;
import lombok.NonNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

final class JLine3Completer implements Completer {

  private final JLine3Console console;

  public JLine3Completer(@NonNull JLine3Console console) {
    this.console = console;
  }

  @Override
  public void complete(@NonNull LineReader reader, @NonNull ParsedLine line, @NonNull List<Candidate> candidates) {
    // iterate over all enabled tab complete handlers and record their completions
    // make sure to pass a sort to the candidate in order to keep the order the same way as given
    var currentCandidateSort = 1;
    for (var completeHandler : this.console.tabCompleteHandlers().values()) {
      if (completeHandler.enabled()) {
        // compute the completions of the handler and add the candidates
        var completions = completeHandler.completeInput(line.line());
        for (var completion : completions) {
          var candidate = new Candidate(completion, completion, null, null, null, null, true, currentCandidateSort++);
          candidates.add(candidate);
        }
      }
    }
  }
}
