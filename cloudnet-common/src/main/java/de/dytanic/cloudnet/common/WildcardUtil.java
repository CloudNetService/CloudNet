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

package de.dytanic.cloudnet.common;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public final class WildcardUtil {

  private WildcardUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Filters all values out of the given {@code inputValues} which are matching the given {@code pattern}.
   *
   * @param inputValues the input values to search trough.
   * @param regex       the regex to use for searching.
   * @param <T>         the type of the input values.
   * @return all input values matching the given pattern.
   * @see #filterWildcard(Collection, String, boolean)
   */
  @NotNull
  public static <T extends INameable> Collection<T> filterWildcard(@NotNull Collection<T> inputValues,
    @NotNull String regex) {
    Preconditions.checkNotNull(inputValues, "inputValues");
    Preconditions.checkNotNull(regex, "regex");

    return filterWildcard(inputValues, regex, true);
  }

  /**
   * Checks if any of the given {@code values} matches the given {@code regex}.
   *
   * @param values the values to search trough.
   * @param regex  the regex to use for searching.
   * @return {@code true} if any of the values matches the given regex.
   * @see #anyMatch(Collection, String, boolean)
   */
  public static boolean anyMatch(@NotNull Collection<? extends INameable> values, @NotNull String regex) {
    Preconditions.checkNotNull(values, "values");
    Preconditions.checkNotNull(regex, "regex");

    return anyMatch(values, regex, true);
  }

  /**
   * Filters all values out of the given {@code inputValues} which are matching the given {@code pattern}.
   *
   * @param inputValues   the input values to search trough.
   * @param regex         the regex to use for searching.
   * @param caseSensitive if the search should be case sensitive.
   * @param <T>           the type of the input values.
   * @return all input values matching the given pattern.
   */
  @NotNull
  public static <T extends INameable> Collection<T> filterWildcard(@NotNull Collection<T> inputValues,
    @NotNull String regex, boolean caseSensitive) {
    Preconditions.checkNotNull(inputValues, "inputValues");
    Preconditions.checkNotNull(regex, "regex");

    if (inputValues.isEmpty()) {
      return inputValues;
    } else {
      Pattern pattern = prepare(regex, caseSensitive);
      return pattern == null ? Collections.emptyList() : inputValues.stream()
        .filter(t -> pattern.matcher(t.getName()).matches())
        .collect(Collectors.toList());
    }
  }

  /**
   * Checks if any of the given {@code values} matches the given {@code regex}.
   *
   * @param values        the values to search trough.
   * @param regex         the regex to use for searching.
   * @param caseSensitive if the search should be case sensitive.
   * @return {@code true} if any of the values matches the given regex.
   */
  public static boolean anyMatch(@NotNull Collection<? extends INameable> values, @NotNull String regex,
    boolean caseSensitive) {
    Preconditions.checkNotNull(values, "values");
    Preconditions.checkNotNull(regex, "regex");

    if (values.isEmpty()) {
      return false;
    } else {
      Pattern pattern = prepare(regex, caseSensitive);
      return pattern != null && values.stream()
        .anyMatch(t -> pattern.matcher(t.getName()).matches());
    }
  }

  /**
   * Prepares the given {@code regex} string, grouping every {@literal *} in the provided string to a separate regex
   * group.
   *
   * @param regex         the regex string to prepare and compile.
   * @param caseSensitive if the pattern should be case-insensitive
   * @return the compiled pattern or {@code null} if the compilation failed
   */
  @Nullable
  private static Pattern prepare(@NotNull String regex, boolean caseSensitive) {
    regex = regex.replace("*", "(.*)");
    return tryCompile(regex, caseSensitive);
  }

  /**
   * Tries to compile the given pattern string and tries to automatically fix it if the input {@code pattern} is not
   * compilable.
   *
   * @param pattern       the pattern string to compile
   * @param caseSensitive if the pattern should be case-insensitive
   * @return the compiled pattern or {@code null} if the compilation failed
   */
  @Nullable
  private static Pattern tryCompile(@NotNull String pattern, boolean caseSensitive) {
    try {
      return Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException exception) {
      return tryFixPattern(exception, caseSensitive);
    } catch (StackOverflowError error) {
      return null;
    }
  }

  /**
   * Tries to automatically fix a pattern based on the given {@code exception}.
   *
   * @param exception     the exception occurred during the compile of the pattern string
   * @param caseSensitive if the pattern check should be case-insensitive
   * @return a fixed, compiled version of the pattern or {@code null} if the given exception is unclear
   */
  private static Pattern tryFixPattern(@NotNull PatternSyntaxException exception, boolean caseSensitive) {
    if (exception.getPattern() != null && exception.getIndex() != -1) {
      String pattern = exception.getPattern();
      if (pattern.length() > exception.getIndex()) {
        // index represents the char before the failed to parsed char index
        String firstPart = pattern.substring(0, exception.getIndex() + 1);
        String secondPart = pattern.substring(exception.getIndex() + 1);
        // escape the specific character which caused the failure using a \ and retry
        return tryCompile(firstPart + '\\' + secondPart, caseSensitive);
      } else if (exception.getDescription() != null
        && exception.getDescription().equals("Unclosed group")
        && exception.getIndex() == pattern.length()) {
        // an unclosed group is a special case which can only occur at the end of the string
        // meaning that a group was opened but not closed, we need to filter that out and escape
        // the group start
        return tryCompile(fixUnclosedGroups(pattern), caseSensitive);
      }
    }
    System.err.println("Unable to fix pattern input " + exception.getPattern());
    exception.printStackTrace();
    return null;
  }

  /**
   * Searches for unclosed groups in the given {@code patternInput} and replaces the group openers {@literal (} with an
   * escaped {@literal \(} while taking care of completed groups.
   *
   * @param patternInput the pattern to check.
   * @return the same pattern as given but with fixed groups.
   */
  @NotNull
  @VisibleForTesting
  protected static String fixUnclosedGroups(@NotNull String patternInput) {
    StringBuilder result = new StringBuilder();
    char[] content = patternInput.toCharArray();
    // we need to record the group closings to actually find the group opening which is not escaped
    int metGroupClosings = 0;
    // we loop reversed over it as we know that the group start must be before the group end and we
    // are searching for it
    for (int index = content.length - 1; index >= 0; index--) {
      char c = content[index];
      if (c == ')' && isPartOfPattern(content, index)) {
        metGroupClosings++;
      } else if (c == '(' && isPartOfPattern(content, index) && --metGroupClosings < 0) {
        // we found an unclosed start of a group, escape it!
        // as we are looping backwards we first need to append the actual char and then the escaping backslash
        result.append(c).append("\\");
        metGroupClosings = 0;
        continue;
      }
      result.append(c);
    }
    // we looped backwards over the string so we need to reverse it to get it back in the correct sequence
    return result.reverse().toString();
  }

  /**
   * Checks if the current content index is part of the pattern or escaped.
   *
   * @param content the whole content of the pattern as char array
   * @param index   the current reader index of the char to check
   * @return {@code true} if the char is part of the pattern or {@code false} if escaped
   */
  private static boolean isPartOfPattern(char[] content, int index) {
    return index <= 0 || content[--index] != '\\';
  }
}
