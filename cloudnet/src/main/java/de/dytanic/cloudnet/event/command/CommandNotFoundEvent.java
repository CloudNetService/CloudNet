package de.dytanic.cloudnet.event.command;

import de.dytanic.cloudnet.driver.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommandNotFoundEvent extends Event {

  private final String commandLine;

}