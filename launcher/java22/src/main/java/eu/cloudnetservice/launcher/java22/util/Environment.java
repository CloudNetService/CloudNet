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

package eu.cloudnetservice.launcher.java22.util;

import com.sun.security.auth.module.NTSystem;
import com.sun.security.auth.module.UnixSystem;
import java.util.Arrays;
import java.util.Locale;

public final class Environment {

  private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

  private static final boolean ROOT_OR_ADMIN_USER;
  private static final String HIGH_INTEGRITY_LEVEL = "S-1-16-12288";

  static {
    if (IS_WINDOWS) {
      // windows check
      ROOT_OR_ADMIN_USER = Arrays.binarySearch(new NTSystem().getGroupIDs(), HIGH_INTEGRITY_LEVEL) >= 0;
    } else {
      // unix check
      var elevated = false;
      if (new UnixSystem().getUid() == 0) {
        // getUid returns 0 as well when running which has no name, so we need to double-check to be sure
        // This is fixed in Java 18, see: https://bugs.openjdk.java.net/browse/JDK-8274721
        try {
          var process = new ProcessBuilder("id", "-u").start();
          process.waitFor();
          // check if the output is equal to 0 (the elevated user id)
          elevated = new String(process.getInputStream().readAllBytes()).trim().equals("0");
        } catch (Exception ignored) {
        }
      }
      ROOT_OR_ADMIN_USER = elevated;
    }
  }

  private Environment() {
    throw new UnsupportedOperationException();
  }

  public static boolean runningAsRootOrAdmin() {
    return ROOT_OR_ADMIN_USER;
  }
}
