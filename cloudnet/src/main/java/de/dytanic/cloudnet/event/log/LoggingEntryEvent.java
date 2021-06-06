package de.dytanic.cloudnet.event.log;

import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;

public final class LoggingEntryEvent extends DriverEvent {

  private final LogEntry logEntry;

  public LoggingEntryEvent(LogEntry logEntry) {
    this.logEntry = logEntry;
  }

  public LogEntry getLogEntry() {
    return this.logEntry;
  }

  @Override
  public boolean isShowDebug() {
    return false;
  }

}
