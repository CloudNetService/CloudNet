package de.dytanic.cloudnet.console.log;

import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.common.logging.LoggingUtils;
import de.dytanic.cloudnet.console.ConsoleColor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.jetbrains.annotations.NotNull;

public final class ColouredLogFormatter implements IFormatter {

  private final DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss.SSS");

  @Override
  public @NotNull String format(@NotNull LogEntry logEntry) {
    StringBuilder builder = new StringBuilder();
    LoggingUtils.printStackTraceToStringBuilder(builder, logEntry.getThrowable());

    StringBuilder stringBuilder = new StringBuilder();

    for (String message : logEntry.getMessages()) {
      if (message != null) {
        stringBuilder
          .append(ConsoleColor.DARK_GRAY)
          .append("[")
          .append(ConsoleColor.WHITE)
          .append(this.dateFormat.format(logEntry.getTimeStamp()))
          .append(ConsoleColor.DARK_GRAY)
          .append("] ")
          .append(logEntry.getLogLevel().isColorized() ? ConsoleColor.RED : ConsoleColor.GRAY)
          .append(logEntry.getLogLevel().getUpperName())
          .append(ConsoleColor.DARK_GRAY)
          .append(": ")
          .append(logEntry.getLogLevel().isColorized() ? ConsoleColor.YELLOW : ConsoleColor.DEFAULT)
          .append(message);
      }
    }

    return stringBuilder.append(builder).toString();
  }
}
