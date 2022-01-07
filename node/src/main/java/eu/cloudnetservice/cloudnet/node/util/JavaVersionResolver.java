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

package eu.cloudnetservice.cloudnet.node.util;

import com.google.common.io.CharStreams;
import eu.cloudnetservice.cloudnet.common.JavaVersion;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * An util class for resolving the java version for a given path.
 */
public final class JavaVersionResolver {

  // https://regex101.com/r/6Z4jZT/1
  private static final Pattern JAVA_REGEX = Pattern.compile("^.* version \"(\\d+)\\.?(\\d+)?\\.?([\\d_]+)?\".*",
    Pattern.MULTILINE | Pattern.DOTALL);

  private JavaVersionResolver() {
    throw new UnsupportedOperationException();
  }

  /**
   * Starts a process and checks the java version of the given input
   *
   * @param input the path to the java executable to check the version for
   * @return the java version of the executable, null if the input is no valid path to an executable
   */
  public static @Nullable JavaVersion resolveFromJavaExecutable(@Nullable String input) {
    // the default java command input can always evaluate in the current runtime version
    if (input == null || input.equals("java")) {
      return JavaVersion.runtimeVersion();
    }

    try {
      var process = Runtime.getRuntime().exec(new String[]{input, "-version"});
      try (Reader reader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)) {
        var matcher = JAVA_REGEX.matcher(CharStreams.toString(reader));
        if (matcher.matches()) {
          var majorVersion = matcher.group(1);
          if (majorVersion.equals("1")) {
            // java 8 has the major version defined after an initial 1.
            // fail below if the java version is '1'
            majorVersion = matcher.groupCount() == 1 ? majorVersion : matcher.group(2);
          }
          // parse the java version from the major version (will return null if java 7 or below)
          return JavaVersion.fromVersion(Integer.parseInt(majorVersion)).orElse(null);
        }
      } finally {
        process.destroyForcibly();
      }
    } catch (IOException ignored) {
    }

    return null;
  }
}
