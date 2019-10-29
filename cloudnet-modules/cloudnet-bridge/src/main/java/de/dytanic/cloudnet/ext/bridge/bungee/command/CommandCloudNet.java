package de.dytanic.cloudnet.ext.bridge.bungee.command;

import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public final class CommandCloudNet extends Command {

    public CommandCloudNet() {
        super("cloudnet", "cloudnet.command.cloudnet", "cloud", "cl");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', BridgeConfigurationProvider.load().getPrefix()) + "/cloudnet <command>"));
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg).append(" ");
        }

        if (sender instanceof ProxiedPlayer) {
            CommandInfo commandInfo = CloudNetDriver.getInstance().getConsoleCommand(stringBuilder.toString());
            if (commandInfo != null && commandInfo.getPermission() != null) {
                if (!sender.hasPermission(commandInfo.getPermission())) {
                    sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                            BridgeConfigurationProvider.load().getMessages().get("command-cloud-sub-command-no-permission")
                                    .replace("%command%", stringBuilder))));
                    return;
                }
            }
        }

        String[] messages = CloudNetDriver.getInstance().sendCommandLine(stringBuilder.toString());

        if (messages != null) {
            for (String message : messages) {
                if (message != null) {
                    sender.sendMessage(TextComponent.fromLegacyText(
                            ChatColor.translateAlternateColorCodes('&', BridgeConfigurationProvider.load().getPrefix() + message)));
                }
            }
        }
    }
}