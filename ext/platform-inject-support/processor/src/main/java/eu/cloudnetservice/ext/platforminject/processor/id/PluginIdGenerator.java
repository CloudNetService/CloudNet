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

package eu.cloudnetservice.ext.platforminject.processor.id;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import lombok.NonNull;

public final class PluginIdGenerator {

  private final int minLength;
  private final int maxLength;

  private final Collection<AllowedCharRange> charRanges = new LinkedList<>();

  private PluginIdGenerator(int minLength, int maxLength) {
    this.minLength = minLength;
    this.maxLength = maxLength;
  }

  public static @NonNull PluginIdGenerator withInfiniteLength() {
    return withBoundedLength(0, -1);
  }

  public static @NonNull PluginIdGenerator withMaxLength(int maxLength) {
    return withBoundedLength(0, maxLength);
  }

  public static @NonNull PluginIdGenerator withMinLength(int minLength) {
    return withBoundedLength(minLength, -1);
  }

  public static @NonNull PluginIdGenerator withBoundedLength(int minLength, int maxLength) {
    if (maxLength != -1 && minLength > maxLength) {
      throw new IllegalArgumentException("minLength > maxLength");
    }
    return new PluginIdGenerator(minLength, maxLength);
  }

  public @NonNull PluginIdGenerator registerRange(
    int indexLowerBound,
    char replacementChar,
    @NonNull CharRange... allowedChars
  ) {
    return this.registerRange(indexLowerBound, -1, replacementChar, allowedChars);
  }

  public @NonNull PluginIdGenerator registerRange(
    int indexLowerBound,
    int indexUpperBound,
    char replacementChar,
    @NonNull CharRange... allowedChars
  ) {
    this.charRanges.add(new AllowedCharRange(allowedChars, indexLowerBound, indexUpperBound, replacementChar));
    return this;
  }

  public @NonNull String convert(@NonNull String input) {
    var chars = input.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      var charAtIndex = chars[i];

      // find the first char range that handles the index at use the char which is produced by it
      // if no range handles the input char we just assume there is nothing to replace
      for (var charRange : this.charRanges) {
        if (charRange.handledByRange(i)) {
          chars[i] = charRange.replaceCharIfOutOfBounds(charAtIndex);
        }
      }
    }

    // ensure the min length of the string
    var charCount = chars.length;
    if (this.minLength > 0 && charCount < this.minLength) {
      throw new IllegalArgumentException(
        "Given input " + input + " does not meet the requirement of " + this.minLength + " min length");
    }

    // remove from the string if it exceeds the max length
    if (this.maxLength > 0 && charCount > this.maxLength) {
      chars = Arrays.copyOf(chars, this.maxLength);
    }

    // construct a new string from the replaced chars
    return new String(chars);
  }
}
