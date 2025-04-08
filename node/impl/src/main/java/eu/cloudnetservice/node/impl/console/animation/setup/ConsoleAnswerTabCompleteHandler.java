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

package eu.cloudnetservice.node.impl.console.animation.setup;

import eu.cloudnetservice.node.impl.console.handler.ConsoleTabCompleteHandler;
import java.util.Collection;
import lombok.NonNull;

final class ConsoleAnswerTabCompleteHandler extends ConsoleTabCompleteHandler {

  private final Collection<String> possibleResults;

  public ConsoleAnswerTabCompleteHandler(@NonNull Collection<String> possibleResults) {
    this.possibleResults = possibleResults;
  }

  @Override
  public @NonNull Collection<String> completeInput(@NonNull String line) {
    if (line.trim().isEmpty()) {
      return this.possibleResults;
    } else if (line.contains(" ")) {
      // get the last query string
      var parts = line.split(" ");
      var tabCompleteQuery = parts[parts.length - 1];
      // filter the entries to find the best match for the query
      return this.possibleResults.stream()
        .filter(result -> result.regionMatches(true, 0, tabCompleteQuery, 0, tabCompleteQuery.length()))
        .toList();
    } else {
      // just do a full search through all responses
      return this.possibleResults.stream()
        .filter(result -> result.regionMatches(true, 0, line, 0, line.length()))
        .toList();
    }
  }
}
