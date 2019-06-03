package de.dytanic.cloudnet.common.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines a new ByteArrayOutputStream that convert the bytes into a message and
 * invokes the in constructor exist logger
 */
@Getter
@RequiredArgsConstructor
public class LogOutputStream extends ByteArrayOutputStream {

  /**
   * The logger for this outputStream
   */
  protected final ILogger logger;

  /**
   * The LogLevel in that the logger should log the incoming message
   */
  protected final LogLevel logLevel;

  @Override
  public void flush() throws IOException {
    String input = toString(StandardCharsets.UTF_8.name());
    this.reset();

    if (input != null && !input.isEmpty() && !input
        .equals(System.lineSeparator())) {
      logger.log(logLevel, input);
    }
  }
}