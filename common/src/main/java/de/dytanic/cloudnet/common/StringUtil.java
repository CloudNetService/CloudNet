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
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;

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
  public static @NotNull String generateRandomString(int length) {
    Preconditions.checkArgument(length > 0, "Can only generate string which is longer to 0 chars");
    // init the backing api
    StringBuilder buffer = new StringBuilder(length);
    ThreadLocalRandom random = ThreadLocalRandom.current();
    // loop over the string and put a new random char from the DEFAULT_ALPHABET_UPPERCASE constant
    for (int i = 0; i < length; i++) {
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
   * @return {@code true} if the given string ends with the given suffix.
   */
  public static boolean endsWithIgnoreCase(@NotNull String s, @NotNull String suffix) {
    int suffixLength = suffix.length();
    return s.regionMatches(true, s.length() - suffixLength, suffix, 0, suffixLength);
  }

  /**
   * Checks if the given string starts with the given prefix ignoring the string casing.
   *
   * @param s      the string to check.
   * @param prefix the prefix to validate the string starts with.
   * @return {@code true} if the given string starts with the given prefix.
   */
  public static boolean startsWithIgnoreCase(@NotNull String s, @NotNull String prefix) {
    return s.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  public static @NotNull String repeat(char c, int times) {
    char[] s = new char[times];
    for (int i = 0; i < times; i++) {
      s[i] = c;
    }
    return new String(s);
  }
}
