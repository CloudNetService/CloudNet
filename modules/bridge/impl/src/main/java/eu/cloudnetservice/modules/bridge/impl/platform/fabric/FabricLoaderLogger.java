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

package eu.cloudnetservice.modules.bridge.impl.platform.fabric;

import lombok.NonNull;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

/**
 * Utility for logging using the fabric-loader builtin logging system.
 *
 * @since 4.0
 */
final class FabricLoaderLogger {

  private static final LogCategory BRIDGE_CATEGORY = LogCategory.createCustom("CloudNet", "Bridge");

  private FabricLoaderLogger() {
    throw new UnsupportedOperationException();
  }

  /**
   * Logs a message at the debug level. Formatting is applied as specified by {@link String#format(String, Object...)}.
   *
   * @param message the message to log.
   * @param args    additional format arguments to apply to the message.
   * @throws NullPointerException if the given message or args array is null.
   */
  public static void debug(@NonNull String message, @NonNull Object... args) {
    Log.debug(BRIDGE_CATEGORY, message, args);
  }

  /**
   * Logs a message at the info level. Formatting is applied as specified by {@link String#format(String, Object...)}.
   *
   * @param message the message to log.
   * @param args    additional format arguments to apply to the message.
   * @throws NullPointerException if the given message or args array is null.
   */
  public static void info(@NonNull String message, @NonNull Object... args) {
    Log.info(BRIDGE_CATEGORY, message, args);
  }
}
