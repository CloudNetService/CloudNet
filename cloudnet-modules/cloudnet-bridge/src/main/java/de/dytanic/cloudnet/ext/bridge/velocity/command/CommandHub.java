package de.dytanic.cloudnet.ext.bridge.velocity.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.optional.qual.MaybePresent;

public final class CommandHub implements Command {

    @Override
    public void execute(@MaybePresent CommandSource source, @NonNull @MaybePresent String[] args) {
        if (!(source instanceof Player)) {
            return;
        }

        Player player = (Player) source;

        if (VelocityCloudNetHelper.isOnAFallbackInstance(player)) {
            source.sendMessage(LegacyComponentSerializer.legacyLinking().deserialize(BridgeConfigurationProvider.load().getMessages().get("command-hub-already-in-hub").replace("&", "§")));
            return;
        }

        VelocityCloudNetHelper.connectToFallback(player, player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null))
                .thenAccept(connectedFallback -> {
                    if (connectedFallback != null) {
                        source.sendMessage(LegacyComponentSerializer.legacyLinking().deserialize(
                                BridgeConfigurationProvider.load().getMessages().get("command-hub-success-connect")
                                        .replace("%server%", connectedFallback.getName())
                                        .replace("&", "§")
                        ));
                    } else {
                        source.sendMessage(LegacyComponentSerializer.legacyLinking().deserialize(BridgeConfigurationProvider.load().getMessages().get("command-hub-no-server-found").replace("&", "§")));
                    }
                });
    }

}
