package de.dytanic.cloudnet.ext.bridge.velocity.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import net.kyori.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.optional.qual.MaybePresent;

public final class CommandHub implements Command {

    @Override
    public void execute(@MaybePresent CommandSource source, @NonNull @MaybePresent String[] args) {
        if (!(source instanceof Player)) return;

        Player player = (Player) source;

        if (VelocityCloudNetHelper.isOnAFallbackInstance(player)) {
            source.sendMessage(TextComponent.of(BridgeConfigurationProvider.load().getMessages().get("command-hub-already-in-hub").replace("&", "§")));
            return;
        }

        String server = VelocityCloudNetHelper.filterServiceForPlayer(player, player.getCurrentServer().get().getServerInfo().getName());

        if (server != null) {
            RegisteredServer registeredServer = VelocityCloudNetHelper.getProxyServer().getServer(server).get();

            if (registeredServer == null) {
                source.sendMessage(TextComponent.of(BridgeConfigurationProvider.load().getMessages().get("command-hub-no-server-found").replace("&", "§")));
                return;
            }

            player.createConnectionRequest(registeredServer).connect();
            source.sendMessage(TextComponent.of(
                    BridgeConfigurationProvider.load().getMessages().get("command-hub-success-connect")
                            .replace("%server%", server + "")
                            .replace("&", "§")
            ));
        } else
            source.sendMessage(TextComponent.of(BridgeConfigurationProvider.load().getMessages().get("command-hub-no-server-found").replace("&", "§")));
    }
}