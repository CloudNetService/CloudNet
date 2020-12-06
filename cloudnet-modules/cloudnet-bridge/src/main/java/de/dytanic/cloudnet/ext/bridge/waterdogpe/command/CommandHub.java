package de.dytanic.cloudnet.ext.bridge.waterdogpe.command;


import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.WaterdogPECloudNetHelper;
import pe.waterdog.command.Command;
import pe.waterdog.command.CommandSender;
import pe.waterdog.command.CommandSettings;
import pe.waterdog.player.ProxiedPlayer;

import java.util.Arrays;

public class CommandHub extends Command {

    public CommandHub(String[] names) {
        super(names[0], CommandSettings.builder().setAliases(Arrays.copyOfRange(names, 1, names.length)).build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            return true;
        }

        ProxiedPlayer proxiedPlayer = (ProxiedPlayer) sender;

        if (WaterdogPECloudNetHelper.isOnMatchingFallbackInstance(proxiedPlayer)) {
            sender.sendMessage(BridgeConfigurationProvider.load().getMessages().get("command-hub-already-in-hub").replace('&', '§'));
            return true;
        }

        WaterdogPECloudNetHelper.connectToFallback(proxiedPlayer, proxiedPlayer.getServer() != null ? proxiedPlayer.getServer().getInfo().getServerName() : null)
                .thenAccept(connectedFallback -> {
                    if (connectedFallback != null) {
                        sender.sendMessage(
                                BridgeConfigurationProvider.load().getMessages().get("command-hub-success-connect")
                                        .replace("%server%", connectedFallback.getName())
                                        .replace('&', '§')
                        );
                    } else {
                        sender.sendMessage(BridgeConfigurationProvider.load().getMessages().get("command-hub-no-server-found").replace('&', '§'));
                    }
                });

        return true;
    }

}
