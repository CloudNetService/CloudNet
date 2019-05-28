package de.dytanic.cloudnet.event.log;

import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class LoggingEntryEvent extends DriverEvent {

  private final LogEntry logEntry;
}