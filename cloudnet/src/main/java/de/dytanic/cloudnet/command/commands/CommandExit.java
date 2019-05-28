package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;

public final class CommandExit extends CommandDefault {

  public CommandExit() {
    super("exit", "shutdown", "stop");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args,
    String commandLine, Properties properties) {
    getCloudNet().stop();
  }
}