/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.common.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class LoggingSupport {

  private static ClassContextualSecurityManager securityManager;
  private static boolean securityManagerInitializationAttempted = false;
  private static final Logger LOGGER = LogManager.getLogger(LoggingSupport.class);

  private LoggingSupport() {
    throw new UnsupportedOperationException();
  }

  public static void reportError(@NotNull String message) {
    LOGGER.severe("ERROR: " + message);
  }

  public static void reportError(@NotNull String message, @NotNull Throwable throwable) {
    LOGGER.severe("ERROR: " + message);
    LOGGER.severe("Reported exception:", throwable);
  }

  public static @Nullable Class<?> getCallingClass() {
    if (securityManagerInitializationAttempted && securityManager == null) {
      return null;
    }

    if (!securityManagerInitializationAttempted) {
      securityManagerInitializationAttempted = true;
      try {
        securityManager = new ClassContextualSecurityManager();
      } catch (SecurityException ignored) {
        return null;
      }
    }

    Class<?>[] trace = securityManager.getClassContext();
    // go back in stack until this class is the current one
    int i;
    for (i = 0; i < trace.length; i++) {
      if (LoggingSupport.class.getName().equals(trace[i].getName())) {
        break;
      }
    }
    // check if we can find a caller in the stack (should always be possible)
    return i >= trace.length || i + 2 >= trace.length ? null : trace[i + 2];
  }

  private static final class ClassContextualSecurityManager extends SecurityManager {

    @Override
    protected @NotNull Class<?>[] getClassContext() {
      return super.getClassContext();
    }
  }
}
