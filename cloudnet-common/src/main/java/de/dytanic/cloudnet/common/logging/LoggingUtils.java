package de.dytanic.cloudnet.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class LoggingUtils {

  private LoggingUtils() {
    throw new UnsupportedOperationException();
  }

  public static void printStackTraceToStringBuilder(StringBuilder builder, Throwable throwable) {
    if (throwable != null) {
      StringWriter writer = new StringWriter();
      throwable.printStackTrace(new PrintWriter(writer));
      builder.append(writer).append(System.lineSeparator());
    }
  }
}
