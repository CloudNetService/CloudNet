package de.dytanic.cloudnet.ext.bridge.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.util.VelocityComponentRenderer;

public final class CommandHub implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            return;
        }

        Player player = (Player) invocation.source();
        if (VelocityCloudNetHelper.isOnMatchingFallbackInstance(player)) {
            player.sendMessage(VelocityComponentRenderer.rawTranslation("command-hub-already-in-hub"));
            return;
        }

        VelocityCloudNetHelper.connectToFallback(
                player,
                player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null)
        ).thenAccept(connectedFallback -> {
            if (connectedFallback != null) {
                player.sendMessage(VelocityComponentRenderer.rawTranslation("command-hub-success-connect",
                        message -> message.replace("%server%", connectedFallback.getName())));
            } else {
                player.sendMessage(VelocityComponentRenderer.rawTranslation("command-hub-no-server-found"));
            }
        });
    }
}
