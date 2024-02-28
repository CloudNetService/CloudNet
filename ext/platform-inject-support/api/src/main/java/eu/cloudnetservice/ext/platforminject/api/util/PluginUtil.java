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

package eu.cloudnetservice.ext.platforminject.api.util;

import lombok.NonNull;

public final class PluginUtil {

  private PluginUtil() {
    throw new UnsupportedOperationException();
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
