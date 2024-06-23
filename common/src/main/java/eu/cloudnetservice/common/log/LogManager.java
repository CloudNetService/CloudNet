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

package eu.cloudnetservice.common.log;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The log manager provides static access to underlying api to shortcut logger creation methods for easier
 * accessibility.
 *
 * @since 4.0
 */
public final class LogManager {

  private LogManager() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the root logger for the system from the currently used logger factory.
   *
   * @return the root logger.
   */
  public static @NonNull Logger rootLogger() {
    return LoggerFactory.getLogger("");
  }

  /**
   * Gets a logger from the current logger factory which has the name of the given class.
   *
   * @param caller the class to get the logger for.
   * @return a logger with the name of the given class.
   * @throws NullPointerException if the given class is null.
   */
  public static @NonNull Logger logger(@NonNull Class<?> caller) {
    return LoggerFactory.getLogger(caller.getName());
  }

  /**
   * Gets a logger from the current logger factory which has the given name.
   *
   * @param name the name of the logger to get.
   * @return a logger with the given name.
   * @throws NullPointerException if the given name is null.
   */
  public static @NonNull Logger logger(@NonNull String name) {
    return LoggerFactory.getLogger(name);
  }
}
