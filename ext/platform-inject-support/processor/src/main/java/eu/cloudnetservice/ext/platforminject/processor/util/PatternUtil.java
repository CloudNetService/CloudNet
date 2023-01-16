/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.processor.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.NonNull;

public final class PatternUtil {

  private PatternUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Set<Pattern> parsePattern(@NonNull String[] candidates) {
    // if there are no patterns, skip this
    if (candidates.length == 0) {
      return Set.of();
    }

    // convert each candidate
    Set<Pattern> parsedPatterns = new HashSet<>(candidates.length);
    for (var candidate : candidates) {
      // check if the name contains a ':' which indicates what type the candidate is of
      var typeDelimiterIndex = candidate.indexOf(':');
      if (typeDelimiterIndex == -1) {
        // just assume it's plain
        parsedPatterns.add(literalPatternWithWildcardEnding(candidate));
        continue;
      }

      // ensure that there are more chars than the delimiter index
      Objects.checkIndex(typeDelimiterIndex + 1, candidate.length());

      // create the pattern based on the type
      var pattern = candidate.substring(typeDelimiterIndex + 1);
      var type = candidate.substring(0, typeDelimiterIndex).toLowerCase(Locale.ROOT);
      switch (type) {
        // regex
        case "r", "regexp", "pattern" -> parsedPatterns.add(Pattern.compile(pattern));
        // plain
        case "p", "plain" -> parsedPatterns.add(literalPatternWithWildcardEnding(candidate));
        // glob
        case "g", "glob" -> {
          var simplePattern = createRegexFromSimpleGlob(pattern);
          parsedPatterns.add(Pattern.compile(simplePattern));
        }
        // fail when an invalid type is given
        default -> throw new IllegalArgumentException("Invalid type for pattern: " + type);
      }
    }

    // parse done
    return parsedPatterns;
  }

  public static @NonNull Pattern literalPatternWithWildcardEnding(@NonNull String pattern) {
    var fullPattern = String.format("^%s.*$", Pattern.quote(pattern));
    return Pattern.compile(fullPattern);
  }

  private static @NonNull String createRegexFromSimpleGlob(@NonNull String glob) {
    var out = new StringBuilder("^");
    for (var i = 0; i < glob.length(); ++i) {
      var c = glob.charAt(i);
      switch (c) {
        case '*' -> out.append(".*");
        case '?' -> out.append('.');
        case '.' -> out.append("\\.");
        case '\\' -> out.append("\\\\");
        default -> out.append(c);
      }
    }

    return out.append('$').toString();
  }
}
