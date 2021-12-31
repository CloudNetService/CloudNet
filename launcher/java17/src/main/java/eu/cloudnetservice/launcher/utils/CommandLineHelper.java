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

package eu.cloudnetservice.launcher.utils;

import java.util.Properties;
import java.util.function.Function;
import lombok.NonNull;

public final class CommandLineHelper {

  private CommandLineHelper() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Properties parseCommandLine(@NonNull String[] args) {
    var properties = new Properties();
    for (var arg : args) {
      // we require an argument to begin with --
      if (arg.startsWith("--")) {
        var parts = arg.replaceFirst("--", "").split("=", 2);
        if (parts.length == 0) {
          throw new IllegalArgumentException("Invalid command line option " + arg);
        }
        // if there is only one part treat it as a boolean
        if (parts.length == 1) {
          properties.setProperty(parts[0], "true");
          continue;
        }
        // just append the key value pair
        properties.setProperty(parts[0], parts[1]);
      }
    }
    // parsed successfully
    return properties;
  }

  public static @NonNull String findProperty(@NonNull Properties properties, @NonNull String key, @NonNull String def) {
    return properties.getProperty(key, System.getProperty("cloudnet." + key, def));
  }

  public static @NonNull <T> T findProperty(
    @NonNull Properties properties,
    @NonNull String key,
    @NonNull String def, @NonNull
    Function<String, T> parser
  ) {
    return parser.apply(findProperty(properties, key, def));
  }
}
