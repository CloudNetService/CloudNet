package de.dytanic.cloudnet.event.command;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.driver.event.Event;

public class CommandPostProcessEvent extends Event {

  private final String commandLine;

  private final ICommandSender commandSender;

  public CommandPostProcessEvent(String commandLine, ICommandSender commandSender) {
    this.commandLine = commandLine;
    this.commandSender = commandSender;
  }

  public String getCommandLine() {
    return this.commandLine;
  }

  public ICommandSender getCommandSender() {
    return this.commandSender;
  }
}
