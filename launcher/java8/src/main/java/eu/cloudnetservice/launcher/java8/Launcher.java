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

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

public final class Launcher {

  public static void main(String[] args) throws Exception {
    int javaVersion = detectJavaVersion();
    if (javaVersion == 25) {
      Class<?> theLauncherClass = Class.forName("eu.cloudnetservice.launcher.java22.CloudNetLauncher");
      Constructor<?> launcherEntrypoint = theLauncherClass.getConstructor(String[].class);
      launcherEntrypoint.newInstance((Object) args);
    } else {
      // CHECKSTYLE.OFF: Launcher has no proper logger
      System.err.println("Cannot start CloudNet: Java 25 is required, but Java " + javaVersion + " was detected");
      System.err.println("Please install a Java 25 JRE or JDK (e.g. from OpenJDK, Azul, Adoptium, or another vendor)");
      TimeUnit.SECONDS.sleep(7L);
      System.exit(1);
      // CHECKSTYLE.ON
    }
  }

  private static int detectJavaVersion() {
    String specificationVersion = System.getProperty("java.specification.version");
    if (specificationVersion.startsWith("1.")) {
      specificationVersion = specificationVersion.replaceFirst("1\\.", "");
    }

    try {
      return Integer.parseInt(specificationVersion);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Unable to determine java version from input " + specificationVersion);
    }
  }
}
