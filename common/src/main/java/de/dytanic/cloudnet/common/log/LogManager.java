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

import java.util.Iterator;
import java.util.ServiceLoader;
import org.jetbrains.annotations.NotNull;

public final class LogManager {

  private static final LoggerFactory LOGGER_FACTORY = loadLoggerFactory();

  private LogManager() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull Logger getRootLogger() {
    return LogManager.getLogger(LoggerFactory.ROOT_LOGGER_NAME);
  }

  public static @NotNull Logger getLogger(@NotNull Class<?> caller) {
    return LogManager.getLogger(caller.getName());
  }

  public static @NotNull Logger getLogger(@NotNull String name) {
    return LogManager.getLoggerFactory().getLogger(name);
  }

  public static @NotNull LoggerFactory getLoggerFactory() {
    return LOGGER_FACTORY;
  }

  private static @NotNull LoggerFactory loadLoggerFactory() {
    Iterator<LoggerFactory> factories = ServiceLoader.load(LoggerFactory.class).iterator();
    // check if a logger service is registered
    if (factories.hasNext()) {
      return factories.next();
    } else {
      return new FallbackLoggingFactory();
    }
  }
}
