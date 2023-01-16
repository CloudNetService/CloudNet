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

package eu.cloudnetservice.common;

import com.google.common.base.Preconditions;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class all around strings to shortcut longer methods within the project.
 */
public final class StringUtil {

  /**
   * A char array of all letters from A to Z and 1 to 9 for the random string generation.
   */
  private static final char[] DEFAULT_ALPHABET_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

  private StringUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates a random string with the provided length.
   *
   * @param length the length the generated string should have.
   * @return the randomly generated string.
   */
  public static @NonNull String generateRandomString(int length) {
    Preconditions.checkArgument(length > 0, "Can only generate string which is longer to 0 chars");
    // init the backing api
    var buffer = new StringBuilder(length);
    var random = ThreadLocalRandom.current();
    // loop over the string and put a new random char from the DEFAULT_ALPHABET_UPPERCASE constant
    for (var i = 0; i < length; i++) {
      buffer.append(DEFAULT_ALPHABET_UPPERCASE[random.nextInt(DEFAULT_ALPHABET_UPPERCASE.length)]);
    }
    // convert to string
    return buffer.toString();
  }

  /**
   * Checks if the given string ends with the given suffix ignoring the string casing.
   *
   * @param s      the string to check.
   * @param suffix the suffix to validate the string ends with.
   * @return true if the given string ends with the given suffix, false otherwise.
   */
  public static boolean endsWithIgnoreCase(@NonNull String s, @NonNull String suffix) {
    var suffixLength = suffix.length();
    return s.regionMatches(true, s.length() - suffixLength, suffix, 0, suffixLength);
  }

  /**
   * Checks if the given string starts with the given prefix ignoring the string casing.
   *
   * @param s      the string to check.
   * @param prefix the prefix to validate the string starts with.
   * @return true if the given string starts with the given prefix, false otherwise.
   */
  public static boolean startsWithIgnoreCase(@NonNull String s, @NonNull String prefix) {
    return s.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  /**
   * Converts all the characters in the given string to lower case using a locale sensitive operation.
   *
   * @param s the string to convert, or null.
   * @return the same string as given with all characters converted to lower case.
   */
  @Contract("!null -> !null; null -> null")
  public static @Nullable String toLower(@Nullable String s) {
    return s == null ? null : s.toLowerCase(Locale.ROOT);
  }

  /**
   * Converts all the characters in the given string to upper case using a locale sensitive operation.
   *
   * @param s the string to convert, or null.
   * @return the same string as given with all characters converted to upper case.
   */
  @Contract("!null -> !null; null -> null")
  public static @Nullable String toUpper(@Nullable String s) {
    return s == null ? null : s.toUpperCase(Locale.ROOT);
  }

  public static @NonNull String repeat(char c, int times) {
    var s = new char[times];
    for (var i = 0; i < times; i++) {
      s[i] = c;
    }
    return new String(s);
  }
}
