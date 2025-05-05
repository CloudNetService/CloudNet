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

package eu.cloudnetservice.node.impl.util;

import eu.cloudnetservice.driver.base.Named;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Small utility class which supports pattern based searching through given input collections and tries to fix mistakes
 * in the given pattern input. Mostly used to match stuff based on (sometimes) invalid user supplied pattern input.
 *
 * @since 4.0
 */
public final class WildcardUtil {

  // all "group" chars which are supported by regex. This array is sorted!
  private static final char[] GROUP_CHARS = new char[]{'(', ')', '[', ']', '{', '}'};

  private WildcardUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Filters all values out of the given inputValues which are matching the given pattern.
   *
   * @param inputValues the input values to search trough.
   * @param regex       the regex to use for searching.
   * @param <T>         the type of the input values.
   * @return all input values matching the given pattern.
   * @throws NullPointerException if the given input collection or regex string is null.
   */
  public static <T extends Named> @NonNull @Unmodifiable Collection<T> filterWildcard(
    @NonNull Collection<T> inputValues,
    @NonNull String regex
  ) {
    return filterWildcard(inputValues, regex, true);
  }

  /**
   * Checks if any of the given values matches the given regex.
   *
   * @param values the values to search trough.
   * @param regex  the regex to use for searching.
   * @return true if any of the values matches the given regex, false otherwise.
   * @throws NullPointerException if the given value collection or regex string is null.
   */
  public static boolean anyMatch(@NonNull Collection<? extends Named> values, @NonNull String regex) {
    return anyMatch(values, regex, true);
  }

  /**
   * Filters all values out of the given inputValues which are matching the given pattern. If the given regex input
   * cannot be parsed the given literal string will be used for matching instead.
   *
   * @param inputValues   the input values to search trough.
   * @param regex         the regex to use for searching.
   * @param caseSensitive if the search should be case-sensitive.
   * @param <T>           the type of the input values.
   * @return all input values matching the given pattern.
   * @throws NullPointerException if the given input collection or regex string is null.
   */
  public static <T extends Named> @NonNull @Unmodifiable Collection<T> filterWildcard(
    @NonNull Collection<T> inputValues,
    @NonNull String regex,
    boolean caseSensitive
  ) {
    if (inputValues.isEmpty()) {
      return inputValues;
    } else {
      var compiledPattern = fixPattern(regex, caseSensitive);
      return inputValues.stream().filter(data -> matches(regex, compiledPattern, data, caseSensitive)).toList();
    }
  }

  /**
   * Checks if any of the given values matches the given regex. If the given regex input cannot be parsed the given
   * literal string will be used for matching instead.
   *
   * @param values        the values to search trough.
   * @param regex         the regex to use for searching.
   * @param caseSensitive if the search should be case-sensitive.
   * @return true if any of the values matches the given regex, false otherwise.
   * @throws NullPointerException if the given input collection or regex string is null.
   */
  public static boolean anyMatch(
    @NonNull Collection<? extends Named> values,
    @NonNull String regex,
    boolean caseSensitive
  ) {
    if (values.isEmpty()) {
      return false;
    } else {
      var compiledPattern = fixPattern(regex, caseSensitive);
      return values.stream().anyMatch(data -> matches(regex, compiledPattern, data, caseSensitive));
    }
  }

  /**
   * Prepares the given regex input by replacing all wildcard characters with pattern-syntax wildcard chars. This method
   * will then try to compile the regex and automatically fix syntax errors in it. Null is returned when the given input
   * can neither be compiled nor fixed.
   *
   * @param regex         the regex string to prepare and compile.
   * @param caseSensitive if the pattern should be case-insensitive.
   * @return the compiled pattern or null if the compilation failed.
   * @throws NullPointerException if the given regex string is null.
   */
  public static @Nullable Pattern fixPattern(@NonNull String regex, boolean caseSensitive) {
    regex = regex.replace("*", ".*");
    return tryCompile(regex, caseSensitive);
  }

  /**
   * Compiles the given pattern string and (when needed) tries to automatically fix syntax errors in it. This method
   * returns null if the given input could not be parsed nor fixed.
   *
   * @param pattern       the pattern string to compile.
   * @param caseSensitive if the pattern should be case-insensitive.
   * @return the compiled pattern or null if the compilation failed.
   * @throws NullPointerException if the given pattern string is null.
   */
  private static @Nullable Pattern tryCompile(@NonNull String pattern, boolean caseSensitive) {
    try {
      return Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException exception) {
      return tryFixPattern(exception, caseSensitive);
    } catch (StackOverflowError error) {
      return null;
    }
  }

  /**
   * Tries to automatically fix a pattern based on the given exception.
   *
   * @param exception     the exception occurred during compilation of the pattern string.
   * @param caseSensitive if the pattern check should be case-insensitive.
   * @return a fixed, compiled version of the pattern or null if the given exception is unclear.
   * @throws NullPointerException if the given syntax exception is null.
   */
  private static @Nullable Pattern tryFixPattern(@NonNull PatternSyntaxException exception, boolean caseSensitive) {
    if (exception.getPattern() != null && exception.getIndex() != -1) {
      // check if we can fix the pattern by inserting a backslash
      // if we are at the last char of the group this will most likely not work
      var pattern = exception.getPattern();
      if (exception.getDescription() != null && exception.getDescription().startsWith("Unclosed")) {
        // try to fix some unclosed stuff based on the given index in the exception
        var fixerResult = fixUnclosedGroups(pattern, exception.getIndex());
        if (fixerResult != null) {
          return tryCompile(fixerResult, caseSensitive);
        }
      } else if (pattern.length() > (exception.getIndex() + 1)) {
        // index represents the char before the failed to parsed char index
        var firstPart = pattern.substring(0, exception.getIndex() + 1);
        var secondPart = pattern.substring(exception.getIndex() + 1);
        // escape the specific character which caused the failure using a \ and retry
        return tryCompile(firstPart + '\\' + secondPart, caseSensitive);
      }
    }
    return null;
  }

  /**
   * Searches for unclosed group chars based on the given error description.
   *
   * @param patternInput the pattern to check.
   * @return the same pattern as given but with fixed groups, null if fixing the group input was not possible.
   * @throws NullPointerException if the given pattern input or error description is null.
   */
  @VisibleForTesting
  static @Nullable String fixUnclosedGroups(@NonNull String patternInput, int idx) {
    if (idx < 0) {
      // nothing we can do
      return null;
    }

    var done = false;
    var result = new StringBuilder();
    var content = patternInput.toCharArray();
    // we loop reversed over it as we know that the group start must be before the group end, and we
    // are searching for it
    for (var index = content.length - 1; index >= 0; index--) {
      var c = content[index];
      // just append until we met the specified index
      if (index > idx || done) {
        result.append(c);
        continue;
      }

      // check if the current char is one of the chars we should escape
      if (Arrays.binarySearch(GROUP_CHARS, c) >= 0 && isPartOfPattern(content, index)) {
        // do not search further
        done = true;
        result.append(c).append("\\");
      } else {
        // nothing special, just append
        result.append(c);
      }
    }
    // we looped backwards over the string, so we need to reverse it to get it back in the correct sequence
    return result.reverse().toString();
  }

  /**
   * Checks if the current content index is part of the pattern or escaped.
   *
   * @param content the whole content of the pattern as char array.
   * @param index   the current reader index of the char to check.
   * @return true if the char is part of the pattern or false if escaped.
   */
  private static boolean isPartOfPattern(char[] content, int index) {
    return index <= 0 || content[--index] != '\\';
  }

  /**
   * Tries to match the given data name using the compiled pattern, falling back to a direct equal check if the given
   * compiled pattern is null.
   *
   * @param patternInput  the input to create the pattern, used for raw checking if needed.
   * @param compiled      the compiled pattern from the given input, null if not able to compile.
   * @param data          the data to check if it matches the given regex.
   * @param caseSensitive if the match check should be case-sensitive.
   * @return true if the given data name matches the given regex, false otherwise.
   * @throws NullPointerException if the given pattern input or data is null.
   */
  private static boolean matches(
    @NonNull String patternInput,
    @Nullable Pattern compiled,
    @NonNull Named data,
    boolean caseSensitive
  ) {
    if (compiled == null) {
      // no compiled pattern, match based on the given pattern input
      return caseSensitive ? patternInput.equals(data.name()) : patternInput.equalsIgnoreCase(data.name());
    } else {
      // compiled pattern present - match based on it
      return compiled.matcher(data.name()).matches();
    }
  }
}
