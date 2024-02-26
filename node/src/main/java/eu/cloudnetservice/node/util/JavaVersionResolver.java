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

package eu.cloudnetservice.node.util;

import com.google.common.primitives.Ints;
import eu.cloudnetservice.common.jvm.JavaVersion;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * An util class for resolving the java version for a given path.
 */
public final class JavaVersionResolver {

  private static final Logger LOGGER = LogManager.logger(JavaVersionResolver.class);

  // https://regex101.com/r/VO0bsk/1
  private static final Pattern JAVA_REGEX = Pattern.compile(
    "^.* version \"(\\d+)\\.?(\\d+)?.*",
    Pattern.MULTILINE | Pattern.DOTALL);

  private JavaVersionResolver() {
    throw new UnsupportedOperationException();
  }

  /**
   * Starts a process and checks the java version of the given java executable path.
   *
   * @param input the path to the java executable to check the version of.
   * @return the java version of the executable, null if not parseable or if the version is unsupported.
   */
  public static @Nullable JavaVersion resolveFromJavaExecutable(@Nullable String input) {
    // no input is always the runtime version
    if (input == null) {
      return JavaVersion.runtimeVersion();
    }

    try {
      var process = Runtime.getRuntime().exec(new String[]{input, "-version"});
      try (var stream = process.getErrorStream()) {
        var matcher = JAVA_REGEX.matcher(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        if (matcher.matches()) {
          var majorVersion = matcher.group(1);
          if (majorVersion.equals("1")) {
            // java 8 has the major version defined after an initial 1.
            // fail below if the java version is '1'
            majorVersion = matcher.groupCount() == 1 ? majorVersion : matcher.group(2);
          }

          // parse the java version from the major version, if the version is a valid number
          var majorVersionNumber = Ints.tryParse(majorVersion);
          if (majorVersionNumber != null) {
            // get the version and check if the version is supported.
            var version = JavaVersion.guessFromMajor(majorVersionNumber);
            return version.supported() ? version : null;
          }
        }
      } finally {
        process.destroyForcibly();
      }
    } catch (IOException exception) {
      LOGGER.warning("Unable to read input from process", exception);
    }

    return null;
  }
}
