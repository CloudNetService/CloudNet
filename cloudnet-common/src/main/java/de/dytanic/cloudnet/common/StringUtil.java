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

import java.util.Random;

/**
 * Includes string operations that are needed. Within the project and which are needed more frequently
 */
public final class StringUtil {


  /**
   * A char array of all letters from A to Z and 1 to 9
   */
  private static final char[] DEFAULT_ALPHABET_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

  private static final Random RANDOM = new Random();

  private StringUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates a random string with a array of chars
   *
   * @param length the length of the generated string
   * @return the string, which was build with all random chars
   */
  public static String generateRandomString(int length) {
    StringBuilder stringBuilder = new StringBuilder();

    synchronized (StringUtil.class) {
      for (int i = 0; i < length; i++) {
        stringBuilder.append(DEFAULT_ALPHABET_UPPERCASE[RANDOM.nextInt(DEFAULT_ALPHABET_UPPERCASE.length)]);
      }
    }

    return stringBuilder.toString();
  }
}
