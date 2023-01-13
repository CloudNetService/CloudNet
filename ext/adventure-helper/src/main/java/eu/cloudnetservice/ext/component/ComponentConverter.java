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

package eu.cloudnetservice.ext.component;

import lombok.NonNull;

public class ComponentConverter<C> {

  final ComponentFormat<?> source;
  final ComponentFormat<C> target;

  ComponentConverter(@NonNull ComponentFormat<?> source, @NonNull ComponentFormat<C> target) {
    this.source = source;
    this.target = target;
  }

  public @NonNull C convert(@NonNull String input) {
    var convertedText = this.convertText(input);
    return this.target.encodeStringToComponent(convertedText);
  }

  public @NonNull String convertText(@NonNull String input) {
    var result = new StringBuilder();
    // find all legacy chars
    var chars = input.toCharArray();
    for (var i = 0; i < chars.length; i++) {
      // check if there is at least one char following the current index
      if (i < chars.length - 1) {
        // check if the next char is a legacy color char
        var curr = chars[i];
        var next = chars[i + 1];

        // check if the next segment is holding legacy color info
        if (this.source.nextSegmentIsLegacyFormatting(chars, i, curr, next)) {
          // legacy formatting segment:
          //   1: append the color indication char of the target format
          //   2: append the legacy color char which is following the current indication char
          //   3: skip the next character of the string (the color char)
          result.append(this.target.colorIndicationChar()).append(next);
          i++;
          continue;
        }

        // check if the segment is holding hex color info
        if (this.source.nextSegmentIsHexadecimalFormatting(chars, i, curr, next)) {
          var sourceHexDel = this.source.usesColorCharAsHexDelimiter();
          var targetHexDel = this.target.usesColorCharAsHexDelimiter();
          // hex formatting segment:
          //   1: append the information indicting that a hex format is following
          result.append(this.target.colorIndicationChar()).append(this.target.hexIndicationChar());
          //   2: loop over the chars (skip the color & hex indication chars)
          //   3: skip over all hex delimiter chars as we don't need to worry about them
          //   4: append the char at the current position, prefixed by the hex delimiter if the target has one
          for (var pos = i + 2; pos < i + this.source.hexSegmentLength(); pos++) {
            var c = chars[pos];
            // skip over delimiter chars - we don't need them
            if (sourceHexDel && this.source.charIsValidColorIndicationChar(chars, pos, c)) {
              continue;
            }

            // append the hex char to the target builder, prefixed by the delimiter char if needed
            if (targetHexDel) {
              result.append(this.target.colorIndicationChar());
            }
            result.append(c);
          }

          //   5: move the cursor the next section of the string
          i += (this.source.hexSegmentLength() - 1);
          continue;
        }
      }

      // no special char, just append it
      result.append(chars[i]);
    }

    // build the final string
    return result.toString();
  }

  public @NonNull <T> ComponentConverter<T> andThen(@NonNull ComponentConverter<T> downstream) {
    return new MappingConverter<>(this, downstream);
  }
}
