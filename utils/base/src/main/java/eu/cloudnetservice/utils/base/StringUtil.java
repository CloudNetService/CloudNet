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

package eu.cloudnetservice.utils.base;

import com.google.common.base.Preconditions;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for working with strings.
 *
 * @since 4.0
 */
public final class StringUtil {

  private static final Random SECURE_RANDOM = new SecureRandom();
  private static final char[] DEFAULT_ALPHABET_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

  private StringUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates a random string consisting of uppercase characters and digits with the provided length.
   *
   * @param length the length the generated string should have.
   * @return the randomly generated string.
   * @throws IllegalArgumentException if the given string length is zero or negative.
   */
  public static @NonNull String generateRandomString(int length) {
    Preconditions.checkArgument(length > 0, "Can only generate string which is longer to 0 chars");

    var buffer = new StringBuilder(length);
    for (var i = 0; i < length; i++) {
      var nextCharIdx = SECURE_RANDOM.nextInt(DEFAULT_ALPHABET_UPPERCASE.length);
      buffer.append(DEFAULT_ALPHABET_UPPERCASE[nextCharIdx]);
    }

    // convert to string
    return buffer.toString();
  }

  /**
   * Checks if the given string ends with the given suffix ignoring the string casing.
   *
   * @param string the string to check for the given suffix.
   * @param suffix the suffix to validate the string ends with.
   * @return true if the given string ends with the given suffix, false otherwise.
   * @throws NullPointerException if the given string or suffix is null.
   */
  public static boolean endsWithIgnoreCase(@NonNull String string, @NonNull String suffix) {
    var suffixLength = suffix.length();
    return string.regionMatches(true, string.length() - suffixLength, suffix, 0, suffixLength);
  }

  /**
   * Checks if the given string starts with the given prefix ignoring the string casing.
   *
   * @param string the string to check for the given prefix.
   * @param prefix the prefix to validate the string starts with.
   * @return true if the given string starts with the given prefix, false otherwise.
   * @throws NullPointerException if the given string or prefix is null.
   */
  public static boolean startsWithIgnoreCase(@NonNull String string, @NonNull String prefix) {
    return string.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  /**
   * Converts all the characters in the given string to lower case using a locale insensitive operation. This method
   * returns null in case the given string is null.
   *
   * @param string the string to convert to lowercase, can be null.
   * @return the same string as given with all characters converted to lower case.
   */
  @Contract("!null -> !null; null -> null")
  public static @Nullable String toLower(@Nullable String string) {
    return string == null ? null : string.toLowerCase(Locale.ROOT);
  }

  /**
   * Converts all the characters in the given string to upper case using a locale insensitive operation.This method
   * returns null in case the given string is null.
   *
   * @param string the string to convert, to uppercase, can be null.
   * @return the same string as given with all characters converted to upper case.
   */
  @Contract("!null -> !null; null -> null")
  public static @Nullable String toUpper(@Nullable String string) {
    return string == null ? null : string.toUpperCase(Locale.ROOT);
  }

  /**
   * Repeats the given character the given amount times and returns a string consisting of the reputation result.
   *
   * @param c     the character to repeat.
   * @param times the amount of times to repeat the character.
   * @return a string consisting of the given amount of times of the given char.
   * @throws IllegalArgumentException if the given time to repeat the char is negative.
   */
  public static @NonNull String repeat(char c, int times) {
    Preconditions.checkArgument(times >= 0, "Can only copy a char 0 or more times, not negative times");

    // fast path: no repeating needed
    if (times == 0) {
      return "";
    }

    // fast path: only one repeat required
    if (times == 1) {
      return Character.toString(c);
    }

    // fill an array with the required amount of reputations and construct a new string from that.
    var s = new char[times];
    Arrays.fill(s, c);
    return new String(s);
  }
}
