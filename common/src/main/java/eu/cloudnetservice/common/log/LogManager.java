/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import java.util.ServiceLoader;
import lombok.NonNull;

/**
 * The log manager provides static access to underlying api to shortcut logger creation methods for easier
 * accessibility.
 *
 * @since 4.0
 */
public final class LogManager {

  private static final LoggerFactory LOGGER_FACTORY = loadLoggerFactory();

  private LogManager() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the root logger for the system from the currently used logger factory.
   *
   * @return the root logger.
   */
  public static @NonNull Logger rootLogger() {
    return LogManager.logger(LoggerFactory.ROOT_LOGGER_NAME);
  }

  /**
   * Gets a logger from the current logger factory which has the name of the given class.
   *
   * @param caller the class to get the logger for.
   * @return a logger with the name of the given class.
   * @throws NullPointerException if the given class is null.
   */
  public static @NonNull Logger logger(@NonNull Class<?> caller) {
    return LogManager.logger(caller.getName());
  }

  /**
   * Gets a logger from the current logger factory which has the given name.
   *
   * @param name the name of the logger to get.
   * @return a logger with the given name.
   * @throws NullPointerException if the given name is null.
   */
  public static @NonNull Logger logger(@NonNull String name) {
    return LogManager.loggerFactory().logger(name);
  }

  /**
   * Gets the current logger factory which the system uses. The factory is statically initialized with the class and
   * will never change once initialized.
   *
   * @return the current selected logger factory.
   */
  public static @NonNull LoggerFactory loggerFactory() {
    return LOGGER_FACTORY;
  }

  /**
   * Selects the logger factory use. This method tries to load all providers for the logger factory class and uses the
   * first one which is available (if any) and uses it as the factory for loggers. If no logger factory service is
   * available on the class path this method falls back to the fallback logger factory which creates loggers wrapping
   * java.util.logging loggers.
   *
   * @return the logger factory to use in the runtime.
   * @throws java.util.ServiceConfigurationError if something went wrong during the service loading or instantiation.
   */
  private static @NonNull LoggerFactory loadLoggerFactory() {
    var factories = ServiceLoader.load(LoggerFactory.class).iterator();
    // check if a logger service is registered
    if (factories.hasNext()) {
      return factories.next();
    } else {
      return new FallbackLoggerFactory();
    }
  }
}
