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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import lombok.NonNull;

/**
 * Utility class for logging to SLF4J from the bootstrap class loader.
 *
 * @since 4.0
 */
final class UnsafeLogUtil {

  private static final Logger LOGGER_WARN;
  private static final boolean LOGGER_WARN_ENABLED;

  private static final Logger LOGGER_DEBUG;
  private static final boolean LOGGER_DEBUG_ENABLED;

  static {
    try {
      // this class, as most of the others in this package, are on the bootstrap class path where
      // the system class path is not available. therefore, we need to load the logging classes from
      // the system class path and use reflection to call the specific logging methods
      var cl = ClassLoader.getSystemClassLoader();
      var loggerClass = Class.forName("org.slf4j.Logger", true, cl);
      var loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory", true, cl);

      // get the slf4j logger instance for this class
      var lookup = MethodHandles.publicLookup();
      var getLoggerMt = MethodType.methodType(loggerClass, String.class);
      var getLoggerMh = lookup.findStatic(loggerFactoryClass, "getLogger", getLoggerMt);
      var logger = getLoggerMh.invoke(UnsafeLogUtil.class.getName());

      // method type for logging methods:
      //   - log: any <name>(String message, Object[] args): void method
      //   - log enabled: any is<level>Enabled(): boolean method
      var logMt = MethodType.methodType(void.class, String.class, Object[].class);
      var isLogEnabledMt = MethodType.methodType(boolean.class);

      var logWarnMh = lookup.findVirtual(loggerClass, "warn", logMt).bindTo(logger);
      LOGGER_WARN = MethodHandleProxies.asInterfaceInstance(Logger.class, logWarnMh);
      LOGGER_WARN_ENABLED = (boolean) lookup.findVirtual(loggerClass, "isWarnEnabled", isLogEnabledMt).invoke(logger);

      var logTraceMh = lookup.findVirtual(loggerClass, "debug", logMt).bindTo(logger);
      LOGGER_DEBUG = MethodHandleProxies.asInterfaceInstance(Logger.class, logTraceMh);
      LOGGER_DEBUG_ENABLED = (boolean) lookup.findVirtual(loggerClass, "isDebugEnabled", isLogEnabledMt).invoke(logger);
    } catch (Throwable exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  /**
   * Get if the WARN log level is enabled.
   *
   * @return true if the WARN log level is enabled, false otherwise.
   */
  public static boolean warnEnabled() {
    return LOGGER_WARN_ENABLED;
  }

  /**
   * Logs the given message at the WARN logging level if it is enabled.
   *
   * @param message the message to log.
   * @param args    optional arguments to render into the message.
   * @throws NullPointerException if the given message is null.
   */
  public static void warn(@NonNull String message, Object... args) {
    LOGGER_WARN.log(message, args);
  }

  /**
   * Get if the DEBUG log level is enabled.
   *
   * @return true if the DEBUG log level is enabled, false otherwise.
   */
  public static boolean debugEnabled() {
    return LOGGER_DEBUG_ENABLED;
  }

  /**
   * Logs the given message at the DEBUG logging level if it is enabled.
   *
   * @param message the message to log.
   * @param args    optional arguments to render into the message.
   * @throws NullPointerException if the given message is null.
   */
  public static void debug(@NonNull String message, Object... args) {
    LOGGER_DEBUG.log(message, args);
  }

  /**
   * A custom logging interface used to delegate logging calls to an underlying SLF4J logger instance.
   *
   * @since 4.0
   */
  @FunctionalInterface // must be a SAM interface
  public interface Logger {

    /**
     * Logs the given message at the underlying level if it is enabled.
     *
     * @param message the message to log.
     * @param args    optional arguments to render into the message.
     */
    void log(@NonNull String message, Object... args);
  }
}
