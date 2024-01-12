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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the logger which is used withing CloudNet. This logger extends from the default java.util.logging logger
 * but provides methods to easier log messages instead of needing to wrap them or use huge method calls.
 *
 * @since 4.0
 */
public abstract class Logger extends java.util.logging.Logger {

  /**
   * Constructs a new logger instance.
   *
   * @param name               the name of the logger, null for anonymous loggers.
   * @param resourceBundleName the resource bundle name used for localizing messages passed to the logger, can be null.
   * @throws java.util.MissingResourceException if the resource bundle is given but no such bundle was found.
   */
  protected Logger(@Nullable String name, @Nullable String resourceBundleName) {
    super(name, resourceBundleName);
  }

  /**
   * Force logs the given record by directly flushing it to the underlying handlers. All other log methods will try to
   * pass the messages to the log record dispatcher first which processes them delayed.
   *
   * @param logRecord the record to log instantly.
   * @throws NullPointerException if the given record is null.
   */
  public abstract void forceLog(@NonNull LogRecord logRecord);

  /**
   * Get the log record dispatcher used by this logger. This method returns null if no dispatcher is set, meaning that
   * all log records which are created by this logger will be written to all handlers directly. If a dispatcher is
   * present all records will be posted to the dispatcher which is responsible to process and post them to all
   * registered handlers.
   *
   * @return the log record dispatcher used by this logger, null if no dispatcher is set.
   */
  public abstract @Nullable LogRecordDispatcher logRecordDispatcher();

  /**
   * Sets the log record dispatcher. If the given dispatcher is null all log records will be passed to the registered
   * handlers directly, if the dispatcher is given it will be responsible to process the given records and pass them to
   * all registered handlers.
   *
   * @param dispatcher the new dispatcher to use, null to remove the current log dispatcher.
   */
  public abstract void logRecordDispatcher(@Nullable LogRecordDispatcher dispatcher);

  /**
   * Logs the given message using the fine level. This method has no effect if the fine level is not enabled for this
   * logger and will not try to format the given message.
   * <p>
   * The message passed to this method must be in a valid formatter format to be formatted. If no arguments are given
   * the message will just get printed out as-is.
   * <p>
   * This method call is equivalent to {@code logger.log(Level.FINE, message, throwable, params)}.
   *
   * @param message   the message to print and format if parameters are given.
   * @param throwable the throwable associated with the current log event, can be null.
   * @param params    the params to format the given message with.
   * @throws NullPointerException             if the given message or an element of the given params array is null.
   * @throws java.util.IllegalFormatException if parameters are present but the given message uses an illegal format.
   */
  public void fine(@NonNull String message, @Nullable Throwable throwable, Object @NonNull ... params) {
    this.log(Level.FINE, message, throwable, params);
  }

  /**
   * Logs the given message using the finer level. This method has no effect if the fine level is not enabled for this
   * logger and will not try to format the given message.
   * <p>
   * The message passed to this method must be in a valid formatter format to be formatted. If no arguments are given
   * the message will just get printed out as-is.
   * <p>
   * This method call is equivalent to {@code logger.log(Level.FINER, message, throwable, params)}.
   *
   * @param message   the message to print and format if parameters are given.
   * @param throwable the throwable associated with the current log event, can be null.
   * @param params    the params to format the given message with.
   * @throws NullPointerException             if the given message or an element of the given params array is null.
   * @throws java.util.IllegalFormatException if parameters are present but the given message uses an illegal format.
   */
  public void finer(@NonNull String message, @Nullable Throwable throwable, Object @NonNull ... params) {
    this.log(Level.FINER, message, throwable, params);
  }

  /**
   * Logs the given message using the finest level. This method has no effect if the fine level is not enabled for this
   * logger and will not try to format the given message.
   * <p>
   * The message passed to this method must be in a valid formatter format to be formatted. If no arguments are given
   * the message will just get printed out as-is.
   * <p>
   * This method call is equivalent to {@code logger.log(Level.FINEST, message, throwable, params)}.
   *
   * @param message   the message to print and format if parameters are given.
   * @param throwable the throwable associated with the current log event, can be null.
   * @param params    the params to format the given message with.
   * @throws NullPointerException             if the given message or an element of the given params array is null.
   * @throws java.util.IllegalFormatException if parameters are present but the given message uses an illegal format.
   */
  public void finest(@NonNull String message, @Nullable Throwable throwable, Object @NonNull ... params) {
    this.log(Level.FINEST, message, throwable, params);
  }

  /**
   * Logs the given message using the severe level. This method has no effect if the fine level is not enabled for this
   * logger and will not try to format the given message.
   * <p>
   * The message passed to this method must be in a valid formatter format to be formatted. If no arguments are given
   * the message will just get printed out as-is.
   * <p>
   * This method call is equivalent to {@code logger.log(Level.SEVERE, message, throwable, params)}.
   *
   * @param message   the message to print and format if parameters are given.
   * @param throwable the throwable associated with the current log event, can be null.
   * @param params    the params to format the given message with.
   * @throws NullPointerException             if the given message or an element of the given params array is null.
   * @throws java.util.IllegalFormatException if parameters are present but the given message uses an illegal format.
   */
  public void severe(@NonNull String message, @Nullable Throwable throwable, Object @NonNull ... params) {
    this.log(Level.SEVERE, message, throwable, params);
  }

  /**
   * Logs the given message using the warning level. This method has no effect if the fine level is not enabled for this
   * logger and will not try to format the given message.
   * <p>
   * The message passed to this method must be in a valid formatter format to be formatted. If no arguments are given
   * the message will just get printed out as-is.
   * <p>
   * This method call is equivalent to {@code logger.log(Level.WARNING, message, throwable, params)}.
   *
   * @param message   the message to print and format if parameters are given.
   * @param throwable the throwable associated with the current log event, can be null.
   * @param params    the params to format the given message with.
   * @throws NullPointerException             if the given message or an element of the given params array is null.
   * @throws java.util.IllegalFormatException if parameters are present but the given message uses an illegal format.
   */
  public void warning(@NonNull String message, @Nullable Throwable throwable, Object @NonNull ... params) {
    this.log(Level.WARNING, message, throwable, params);
  }

  /**
   * Logs the given message using the info level. This method has no effect if the fine level is not enabled for this
   * logger and will not try to format the given message.
   * <p>
   * The message passed to this method must be in a valid formatter format to be formatted. If no arguments are given
   * the message will just get printed out as-is.
   * <p>
   * This method call is equivalent to {@code logger.log(Level.INFO, message, throwable, params)}.
   *
   * @param message   the message to print and format if parameters are given.
   * @param throwable the throwable associated with the current log event, can be null.
   * @param params    the params to format the given message with.
   * @throws NullPointerException             if the given message or an element of the given params array is null.
   * @throws java.util.IllegalFormatException if parameters are present but the given message uses an illegal format.
   */
  public void info(@NonNull String message, @Nullable Throwable throwable, Object @NonNull ... params) {
    this.log(Level.INFO, message, throwable, params);
  }

  /**
   * Logs the given message using the config level. This method has no effect if the fine level is not enabled for this
   * logger and will not try to format the given message.
   * <p>
   * The message passed to this method must be in a valid formatter format to be formatted. If no arguments are given
   * the message will just get printed out as-is.
   * <p>
   * This method call is equivalent to {@code logger.log(Level.CONFIG, message, throwable, params)}.
   *
   * @param message   the message to print and format if parameters are given.
   * @param throwable the throwable associated with the current log event, can be null.
   * @param params    the params to format the given message with.
   * @throws NullPointerException             if the given message or an element of the given params array is null.
   * @throws java.util.IllegalFormatException if parameters are present but the given message uses an illegal format.
   */
  public void config(@NonNull String message, @Nullable Throwable throwable, Object @NonNull ... params) {
    this.log(Level.CONFIG, message, throwable, params);
  }

  /**
   * Logs the given message using the provided level. This method has no effect if the fine level is not enabled for
   * this logger and will not try to format the given message.
   * <p>
   * The message passed to this method must be in a valid formatter format to be formatted. If no arguments are given
   * the message will just get printed out as-is.
   *
   * @param level   the level of the log record to log.
   * @param message the message to print and format if parameters are given.
   * @param th      the throwable associated with the current log event, can be null.
   * @param params  the params to format the given message with.
   * @throws NullPointerException             if the given level, message or an element of the params array is null.
   * @throws java.util.IllegalFormatException if parameters are present but the given message uses an illegal format.
   */
  public void log(@NonNull Level level, @NonNull String message, @Nullable Throwable th, Object @NonNull ... params) {
    // prevent formatting of messages which cannot be logged by this logger anyway
    if (this.isLoggable(level)) {
      this.log(level, params.length == 0 ? message : String.format(message, params), th);
    }
  }
}
