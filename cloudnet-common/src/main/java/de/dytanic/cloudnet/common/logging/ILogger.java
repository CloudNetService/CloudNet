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

package de.dytanic.cloudnet.common.logging;

/**
 * The logger is designed to handle the application and has all the basic capabilities to provide easy-to-use logging A
 * logger can optionally process messages asynchronously. However, it is not an obligation for the implementation
 */
public interface ILogger extends ILogHandlerProvider<ILogger>, ILevelable, AutoCloseable {

  /**
   * Set the LogLevel notification level. If the level higher or same than the incoming LogLevel.level. The LogEntry is
   * allowed to handle
   *
   * @param level the level, that should set
   * @see LogLevel
   */
  void setLevel(int level);

  /**
   * Allows to post one LogEntry object into the logger, which invokes the LogHandlers for this LogEntry instance
   *
   * @param logEntry the entry, that should be handle
   * @return the current logger instance
   */
  ILogger log(LogEntry logEntry);

  /**
   * Allows to post zero or more LogEntries into the logger, which invokes the LogHandlers for this LogEntry instances
   *
   * @param logEntries the entries, that should be handle
   * @return the current logger instance
   */
  ILogger log(LogEntry... logEntries);

  /**
   * Indicates, that the implementation of the logger has support for asynchronously log handle
   *
   * @return true when the implementation has the support or false if the class doesn't has any async features
   */
  boolean hasAsyncSupport();


  /**
   * A shortcut method from setLevel(level.getLevel()) to setLevel(level)
   *
   * @param level the LogLevel instance, from that the level integer value should get
   */
  default void setLevel(LogLevel level) {
    if (level == null) {
      return;
    }

    this.setLevel(level.getLevel());
  }

  /**
   * An wrapper method for the last base method log(LogEntry) This method should be a simply shortcut and bypasses an
   * implementation of a LogEntry object
   */
  default ILogger log(LogLevel level, String message) {
    return this.log(level, new String[]{message});
  }

  /**
   * An wrapper method for the last base method log(LogEntry) This method should be a simply shortcut and bypasses an
   * implementation of a LogEntry object
   *
   * @see LogEntry
   * @see ILogHandler
   */
  default ILogger log(LogLevel level, String... messages) {
    return this.log(level, messages, null);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) This method should be a simply shortcut and bypasses an
   * implementation of a LogEntry object
   *
   * @see LogEntry
   * @see ILogHandler
   */
  default ILogger log(LogLevel level, Class<?> clazz, String message) {
    return this.log(level, clazz, new String[]{message});
  }

  /**
   * An wrapper method for the last base method log(LogEntry) This method should be a simply shortcut and bypasses an
   * implementation of a LogEntry object
   *
   * @see LogEntry
   * @see ILogHandler
   */
  default ILogger log(LogLevel level, Class<?> clazz, String... messages) {
    return this.log(level, clazz, messages, null);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) This method should be a simply shortcut and bypasses an
   * implementation of a LogEntry object
   *
   * @see LogEntry
   * @see ILogHandler
   */
  default ILogger log(LogLevel level, String message, Throwable throwable) {
    return this.log(level, new String[]{message}, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) This method should be a simply shortcut and bypasses an
   * implementation of a LogEntry object
   *
   * @see LogEntry
   * @see ILogHandler
   */
  default ILogger log(LogLevel level, String[] messages, Throwable throwable) {
    return this.log(level, Thread.currentThread().getContextClassLoader().getClass(), messages, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) This method should be a simply shortcut and bypasses an
   * implementation of a LogEntry object
   *
   * @see LogEntry
   * @see ILogHandler
   */
  default ILogger log(LogLevel level, Class<?> clazz, String message, Throwable throwable) {
    return this.log(level, clazz, new String[]{message}, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) This method should be a simply shortcut and bypasses an
   * implementation of a LogEntry object
   *
   * @see LogEntry
   * @see ILogHandler
   */
  default ILogger log(LogLevel level, Class<?> clazz, String[] messages, Throwable throwable) {
    return this
      .log(new LogEntry(System.currentTimeMillis(), clazz, messages, level, throwable, Thread.currentThread()));
  }


  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel INFO
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger info(String message) {
    return this.log(LogLevel.INFO, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel INFO
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger info(String... messages) {
    return this.log(LogLevel.INFO, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel INFO
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger info(String message, Class<?> clazz) {
    return this.log(LogLevel.INFO, clazz, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel INFO
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger info(String[] messages, Class<?> clazz) {
    return this.log(LogLevel.INFO, clazz, messages);
  }


  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel WARNING
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger warning(String message) {
    return this.log(LogLevel.WARNING, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel WARNING
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger warning(String... messages) {
    return this.log(LogLevel.WARNING, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel WARNING
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger warning(String message, Class<?> clazz) {
    return this.log(LogLevel.WARNING, clazz, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel WARNING
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger warning(String[] messages, Class<?> clazz) {
    return this.log(LogLevel.WARNING, clazz, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel WARNING
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger warning(String message, Throwable throwable) {
    return this.log(LogLevel.WARNING, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel WARNING
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger warning(String[] messages, Throwable throwable) {
    return this.log(LogLevel.WARNING, messages, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel WARNING
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger warning(String message, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.WARNING, clazz, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel WARNING
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger warning(String[] messages, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.WARNING, clazz, messages, throwable);
  }


  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel FATAL
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger fatal(String message) {
    return this.log(LogLevel.FATAL, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel FATAL
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger fatal(String... messages) {
    return this.log(LogLevel.FATAL, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel FATAL
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger fatal(String message, Class<?> clazz) {
    return this.log(LogLevel.FATAL, clazz, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel FATAL
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger fatal(String[] messages, Class<?> clazz) {
    return this.log(LogLevel.FATAL, clazz, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel FATAL
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger fatal(String message, Throwable throwable) {
    return this.log(LogLevel.FATAL, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel FATAL
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger fatal(String[] messages, Throwable throwable) {
    return this.log(LogLevel.FATAL, messages, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel FATAL
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger fatal(String message, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.FATAL, clazz, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel FATAL
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger fatal(String[] messages, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.FATAL, clazz, messages, throwable);
  }


  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel ERROR
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger error(String message) {
    return this.log(LogLevel.ERROR, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel ERROR
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger error(String... messages) {
    return this.log(LogLevel.ERROR, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel ERROR
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger error(String message, Class<?> clazz) {
    return this.log(LogLevel.ERROR, clazz, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel ERROR
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger error(String[] messages, Class<?> clazz) {
    return this.log(LogLevel.ERROR, clazz, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel ERROR
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger error(String message, Throwable throwable) {
    return this.log(LogLevel.ERROR, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel ERROR
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger error(String[] messages, Throwable throwable) {
    return this.log(LogLevel.ERROR, messages, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel ERROR
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger error(String message, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.ERROR, clazz, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel ERROR
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger error(String[] messages, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.ERROR, clazz, messages, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel EXTENDED
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger extended(String message) {
    return this.log(LogLevel.EXTENDED, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel EXTENDED
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger extended(String... messages) {
    return this.log(LogLevel.EXTENDED, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel EXTENDED
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger extended(String message, Class<?> clazz) {
    return this.log(LogLevel.EXTENDED, clazz, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel EXTENDED
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger extended(String[] messages, Class<?> clazz) {
    return this.log(LogLevel.EXTENDED, clazz, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel EXTENDED
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger extended(String message, Throwable throwable) {
    return this.log(LogLevel.EXTENDED, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel EXTENDED
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger extended(String[] messages, Throwable throwable) {
    return this.log(LogLevel.EXTENDED, messages, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel EXTENDED
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger extended(String message, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.EXTENDED, clazz, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel EXTENDED
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger extended(String[] messages, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.EXTENDED, clazz, messages, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel DEBUG
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger debug(String message) {
    return this.log(LogLevel.DEBUG, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel DEBUG
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger debug(String... messages) {
    return this.log(LogLevel.DEBUG, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel DEBUG
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger debug(String message, Class<?> clazz) {
    return this.log(LogLevel.DEBUG, clazz, message);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel DEBUG
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger debug(String[] messages, Class<?> clazz) {
    return this.log(LogLevel.DEBUG, clazz, messages);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel DEBUG
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger debug(String message, Throwable throwable) {
    return this.log(LogLevel.DEBUG, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel DEBUG
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger debug(String[] messages, Throwable throwable) {
    return this.log(LogLevel.DEBUG, messages, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel DEBUG
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger debug(String message, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.DEBUG, clazz, message, throwable);
  }

  /**
   * An wrapper method for the last base method log(LogEntry) It has the default LogLevel DEBUG
   * <p>
   * This method should be a simply shortcut and bypasses an implementation of a LogEntry object
   *
   * @see LogEntry
   * @see LogLevel
   * @see ILogHandler
   */
  default ILogger debug(String[] messages, Class<?> clazz, Throwable throwable) {
    return this.log(LogLevel.DEBUG, clazz, messages, throwable);
  }
}
