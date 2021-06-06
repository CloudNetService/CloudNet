package de.dytanic.cloudnet.event.command;

import de.dytanic.cloudnet.driver.event.Event;

public class CommandNotFoundEvent extends Event {

  private final String commandLine;

  public CommandNotFoundEvent(String commandLine) {
    this.commandLine = commandLine;
  }

  public String getCommandLine() {
    return this.commandLine;
  }
}
