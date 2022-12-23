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

package eu.cloudnetservice.ext.platforminject.api.util;

import java.util.Arrays;
import lombok.NonNull;

public final class PluginUtil {

  private PluginUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String convertNameToId(@NonNull String pluginName) {
    var chars = pluginName.toCharArray();

    // construct a new char array to which we write all converted chars of the plugin id
    // we can use the same amount of chars as we're not removing any char from the name, only replace
    var idChars = new char[chars.length];
    for (int i = 0; i < chars.length; i++) {
      // check if the current char is a lowercase char (allowed at all positions)
      var charAtPos = chars[i];
      if ((charAtPos >= 'a' && charAtPos <= 'z')) {
        idChars[i] = charAtPos;
        continue;
      }

      // check if we are at the first char, in that case we need to prefix the name
      // with a different char as a lowercase one is required at the first position
      if (i == 0) {
        if (charAtPos >= 'A' && charAtPos <= 'Z') {
          idChars[i] = Character.toLowerCase(charAtPos);
        } else {
          idChars[i] = 'a';
        }
        continue;
      }

      // check if the char is within the allowed characters which aren't checked yet
      if ((charAtPos >= 'A' && charAtPos <= 'Z')
        || (charAtPos >= '0' && charAtPos <= '9')
        || charAtPos == '-'
        || charAtPos == '_'
      ) {
        idChars[i] = charAtPos;
        continue;
      }

      // we need to replace the char at the current position
      idChars[i] = '_';
    }

    // check if we need to substring the id
    if (idChars.length > 64) {
      // to long, substring
      var includedChars = Arrays.copyOf(idChars, 64);
      return new String(includedChars);
    } else {
      // size is within bounds
      return new String(idChars);
    }
  }

  public static @NonNull String buildClassName(
    @NonNull String pluginName,
    @NonNull String platformName,
    @NonNull String suffix
  ) {
    // capitalize the first char of the platform name if needed
    if (platformName.length() > 1) {
      var firstChar = platformName.charAt(0);
      if (Character.isLowerCase(firstChar)) {
        platformName = Character.toUpperCase(firstChar) + platformName.substring(1);
      }
    }

    // concat the platform name with the plugin name before we start (we need to validate both)
    var fullName = platformName + pluginName;

    // validate the name
    var builder = new StringBuilder();
    for (char c : fullName.toCharArray()) {
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '$' || c == '_') {
        // char is valid
        builder.append(c);
      } else {
        // just replace with an underscore
        builder.append('_');
      }
    }

    // finish the name
    return String.format("Generated%s%s", builder, suffix);
  }
}
