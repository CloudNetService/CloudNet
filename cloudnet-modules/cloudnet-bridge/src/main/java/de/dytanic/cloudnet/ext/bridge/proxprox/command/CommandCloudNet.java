package de.dytanic.cloudnet.ext.bridge.proxprox.command;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import io.gomint.proxprox.api.ChatColor;
import io.gomint.proxprox.api.command.Command;
import io.gomint.proxprox.api.command.CommandSender;

public final class CommandCloudNet extends Command {

  public CommandCloudNet() {
    super("cloudnet", "dispatch the commandline of the CloudNet node console",
        "cloud", "cl");
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (!sender.hasPermission("cloudnet.command.cloudnet")) {
      return;
    }

    if (args.length == 0) {
      sender.sendMessage(
          ChatColor.toANSI(BridgeConfigurationProvider.load().getPrefix())
              + "/cloudnet <command>");
      return;
    }

    StringBuilder stringBuilder = new StringBuilder();
    for (String arg : args) {
      stringBuilder.append(arg).append(" ");
    }

    String[] messages = CloudNetDriver.getInstance()
        .sendCommandLine(stringBuilder.toString());

    if (messages != null) {
      for (String message : messages) {
        if (message != null) {
          sender.sendMessage(
              ChatColor.toANSI(
                  BridgeConfigurationProvider.load().getPrefix() + message)
          );
        }
      }
    }
  }
}