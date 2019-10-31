package de.dytanic.cloudnet.ext.bridge.proxprox.command;

import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import io.gomint.proxprox.api.ChatColor;
import io.gomint.proxprox.api.command.Command;
import io.gomint.proxprox.api.command.CommandSender;
import io.gomint.proxprox.api.entity.Player;

public final class CommandCloudNet extends Command {

    public CommandCloudNet() {
        super("cloudnet", "dispatch the commandline of the CloudNet node console", "cloud", "cl");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cloudnet.command.cloudnet")) {
            return;
        }

        if (args.length == 0) {
            sender.sendMessage((BridgeConfigurationProvider.load().getPrefix().replace("&", "§")) + "/cloudnet <command>");
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg).append(" ");
        }

        if (sender instanceof Player) {
            CommandInfo commandInfo = CloudNetDriver.getInstance().getConsoleCommand(stringBuilder.toString());
            if (commandInfo != null && commandInfo.getPermission() != null) {
                if (!sender.hasPermission(commandInfo.getPermission())) {
                    sender.sendMessage(
                            BridgeConfigurationProvider.load().getMessages().get("command-cloud-sub-command-no-permission")
                                    .replace("%command%", stringBuilder)
                                    .replace("&", "§")
                    );
                    return;
                }
            }
        }

        String[] messages = CloudNetDriver.getInstance().sendCommandLine(stringBuilder.toString());

        if (messages != null) {
            for (String message : messages) {
                if (message != null) {
                    sender.sendMessage(
                            ChatColor.toANSI(BridgeConfigurationProvider.load().getPrefix() + message)
                    );
                }
            }
        }
    }
}