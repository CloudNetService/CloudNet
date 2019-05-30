package de.dytanic.cloudnet.common.logging;

/**
 * A LogHandler should handle an incoming LogEntry. The Operation can be execute
 * asynchronously or synchronously. It depends on the implementation of the
 * logger and the setting of the LogLevel
 */
public interface ILogHandler extends AutoCloseable {

  /**
   * Allows to handle this incoming LogEntry from the logger This method can
   * invoked asynchronously or synchronously. It depends on the implementation
   * of the logger and the setting of the LogLevel
   *
   * @param logEntry the new incoming log entry
   */
  void handle(LogEntry logEntry);

}