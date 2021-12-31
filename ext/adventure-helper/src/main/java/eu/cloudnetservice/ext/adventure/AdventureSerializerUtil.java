/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.adventure;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class AdventureSerializerUtil {

  public static final char HEX_CHAR = '#';
  public static final char COLOR_CHAR = '§';
  public static final char LEGACY_CHAR = '&';
  public static final char BUNGEE_HEX_CHAR = 'x';

  private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
    .character(COLOR_CHAR)
    .extractUrls()
    .hexColors()
    .build();

  private AdventureSerializerUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String serializeToString(@NonNull String textToSerialize) {
    var result = new StringBuilder();
    // find all legacy chars
    var chars = textToSerialize.toCharArray();
    for (var i = 0; i < chars.length; i++) {
      // check if there is at least one char following the current index
      if (i < chars.length - 1) {
        // check if the next char is a legacy color char
        var next = chars[i + 1];
        // check if the current char is a legacy text char
        if (chars[i] == LEGACY_CHAR) {
          // check if the next char is a legacy color char
          if ((next >= '0' && next <= '9') || (next >= 'a' && next <= 'f') || next == 'r') {
            result.append(COLOR_CHAR);
            continue;
          }
          // check if the next char is a hex begin char
          // 7 because of current hex_char 6_digit_hex (for example &#000fff)
          if (next == HEX_CHAR && i + 7 < chars.length) {
            result.append(COLOR_CHAR);
            continue;
          }
        }
        // check for the stupid bungee cord chat hex format - do this check for both formats, '&' and '§'
        // 13 because of current hex_char 12_digit_hex (for example &x&0&0&0&f&f&f)
        if ((chars[i] == COLOR_CHAR || chars[i] == LEGACY_CHAR) && next == BUNGEE_HEX_CHAR && i + 13 < chars.length) {
          // open the modern hex format
          result.append(COLOR_CHAR).append(HEX_CHAR);
          // replace the terrible format
          // begin at i+3 to skip the initial &x
          // end at i+14 because the hex format is 14 chars long
          // pos+=2 to skip each &
          for (var pos = i + 3; pos < i + 14; pos += 2) {
            result.append(chars[pos]);
          }
          // skip over the hex thing and continue there
          i += 13;
          continue;
        }
      }
      // append just the char at the position
      result.append(chars[i]);
    }
    return result.toString();
  }

  public static @NonNull Component serialize(@NonNull String input) {
    // serialize the text now
    return SERIALIZER.deserialize(serializeToString(input));
  }
}
