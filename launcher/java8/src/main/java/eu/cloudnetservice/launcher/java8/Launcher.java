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

package eu.cloudnetservice.launcher.java8;

public final class Launcher {

  public static void main(String[] args) throws Exception {
    // check if we're at least on java 22
    if (detectJavaVersion() >= 22) {
      Class.forName("eu.cloudnetservice.launcher.java22.CloudNetLauncher")
        .getConstructor(String[].class)
        .newInstance((Object) args);
    } else {
      // CHECKSTYLE.OFF: Launcher has no proper logger
      System.err.println("CloudNet requires Java 22 (or newer). Download it from https://adoptium.net/");
      System.exit(1);
      // CHECKSTYLE.ON
    }
  }

  private static int detectJavaVersion() {
    String specificationVersion = System.getProperty("java.specification.version");
    // java versions < 9 used 1.X - check that
    if (specificationVersion.startsWith("1.")) {
      specificationVersion = specificationVersion.replaceFirst("1\\.", "");
    }
    // we should be able to just parse to an int
    try {
      return Integer.parseInt(specificationVersion);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Unable to determine java version from input " + specificationVersion);
    }
  }
}
