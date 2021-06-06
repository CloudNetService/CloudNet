package de.dytanic.cloudnet.event.command;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.ICancelable;

public class CommandPreProcessEvent extends Event implements ICancelable {

  private final String commandLine;

  private final ICommandSender commandSender;

  private boolean cancelled = false;

  public CommandPreProcessEvent(String commandLine, ICommandSender commandSender) {
    this.commandLine = commandLine;
    this.commandSender = commandSender;
  }

  public String getCommandLine() {
    return this.commandLine;
  }

  public ICommandSender getCommandSender() {
    return this.commandSender;
  }

  public boolean isCancelled() {
    return this.cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
