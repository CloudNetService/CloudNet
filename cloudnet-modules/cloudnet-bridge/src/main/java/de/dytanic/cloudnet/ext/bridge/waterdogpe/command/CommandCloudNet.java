package de.dytanic.cloudnet.ext.bridge.waterdogpe.command;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import pe.waterdog.command.Command;
import pe.waterdog.command.CommandSender;
import pe.waterdog.command.CommandSettings;
import pe.waterdog.player.ProxiedPlayer;

public class CommandCloudNet extends Command {

    public CommandCloudNet() {
        super("cloudnet", CommandSettings.builder()
                .setPermission("cloudnet.command.cloudnet")
                .setUsageMessage(BridgeConfigurationProvider.load().getPrefix().replace('&', '§') + "/cloudnet <command>")
                .setAliases(new String[]{"cloud", "cl"})
                .build()
        );
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        if (args.length == 0) {
            return false;
        }

        String commandLine = String.join(" ", args);

        if (sender instanceof ProxiedPlayer) {
            CommandInfo commandInfo = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(commandLine);

            if (commandInfo != null && commandInfo.getPermission() != null) {
                if (!sender.hasPermission(commandInfo.getPermission())) {
                    sender.sendMessage(BridgeConfigurationProvider.load().getMessages().get("command-cloud-sub-command-no-permission")
                            .replace("%command%", commandLine).replace('&', '§'));
                    return true;
                }
            }
        }

        CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLineAsync(commandLine).onComplete(messages -> {
            for (String message : messages) {
                if (message != null) {
                    sender.sendMessage((BridgeConfigurationProvider.load().getPrefix() + message).replace('&', '§'));
                }
            }
        });

        return true;
    }
}
