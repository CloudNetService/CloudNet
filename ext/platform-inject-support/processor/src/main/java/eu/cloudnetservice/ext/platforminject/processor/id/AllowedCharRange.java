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

import lombok.NonNull;

record AllowedCharRange(@NonNull CharRange[] allowedCharRanges, int lowerBound, int upperBound, char replacementChar) {

  public boolean handledByRange(int index) {
    return index >= this.lowerBound && (this.upperBound == -1 || index <= this.upperBound);
  }

  public char replaceCharIfOutOfBounds(char characterAtIndex) {
    return this.replaceCharIfOutOfBounds(characterAtIndex, true);
  }

  private char replaceCharIfOutOfBounds(char characterAtIndex, boolean tryLowerCase) {
    // check if there is one char range that is satisfied by the given char
    for (var charRange : this.allowedCharRanges) {
      if (charRange.inRange(characterAtIndex)) {
        return characterAtIndex;
      }
    }

    // if we should try this with lower-cased version of the char, do that
    var lowerCaseVersion = Character.toLowerCase(characterAtIndex);
    if (characterAtIndex != lowerCaseVersion && tryLowerCase) {
      return this.replaceCharIfOutOfBounds(lowerCaseVersion, false);
    }

    // char at index didn't pass one check, use the replacement instead
    return this.replacementChar;
  }
}
