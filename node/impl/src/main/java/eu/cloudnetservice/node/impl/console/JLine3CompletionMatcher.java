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

import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.Candidate;
import org.jline.reader.CompletingParsedLine;
import org.jline.reader.CompletionMatcher;
import org.jline.reader.LineReader;

final class JLine3CompletionMatcher implements CompletionMatcher {

  private volatile List<Candidate> candidates;
  private volatile CompletingParsedLine parsedLine;

  @Override
  public void compile(
    @NonNull Map<LineReader.Option, Boolean> options,
    boolean prefix,
    @NonNull CompletingParsedLine line,
    boolean caseInsensitive,
    int errors,
    @Nullable String originalGroupName
  ) {
    this.candidates = null;
    this.parsedLine = line;
  }

  @Override
  public @NonNull List<Candidate> matches(@NonNull List<Candidate> candidates) {
    this.candidates = candidates;
    return candidates;
  }

  @Override
  public @Nullable Candidate exactMatch() {
    // keep a local copy of the variables in case of concurrent calls
    var candidates = Objects.requireNonNull(this.candidates);
    var parsedLine = Objects.requireNonNull(this.parsedLine);

    // check if there is a 100% match
    var givenWord = parsedLine.word();
    for (var candidate : candidates) {
      if (candidate.complete() && givenWord.equalsIgnoreCase(candidate.value())) {
        return candidate;
      }
    }

    // no exact match
    return null;
  }

  @Override
  public @Nullable String getCommonPrefix() {
    // keep a local copy of the candidates in case of concurrent calls
    var candidates = Objects.requireNonNull(this.candidates);

    String commonPrefix = null;
    for (var candidate : candidates) {
      if (candidate.complete()) {
        if (commonPrefix == null) {
          // no common prefix yet
          commonPrefix = candidate.value();
        } else {
          // get the common prefix
          commonPrefix = Strings.commonPrefix(commonPrefix, candidate.value());
        }
      }
    }
    return commonPrefix;
  }
}
