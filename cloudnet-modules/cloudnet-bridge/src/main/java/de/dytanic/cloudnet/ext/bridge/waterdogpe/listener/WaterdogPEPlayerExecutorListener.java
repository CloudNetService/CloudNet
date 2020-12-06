package de.dytanic.cloudnet.ext.bridge.waterdogpe.listener;

import de.dytanic.cloudnet.ext.bridge.listener.PlayerExecutorListener;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.WaterdogPECloudNetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pe.waterdog.ProxyServer;
import pe.waterdog.network.ServerInfo;
import pe.waterdog.player.ProxiedPlayer;

import java.util.Collection;
import java.util.UUID;

public class WaterdogPEPlayerExecutorListener extends PlayerExecutorListener<ProxiedPlayer> {

    @Nullable
    @Override
    protected ProxiedPlayer getPlayer(@NotNull UUID uniqueId) {
        return ProxyServer.getInstance().getPlayer(uniqueId);
    }

    @Override
    protected @NotNull Collection<ProxiedPlayer> getOnlinePlayers() {
        return ProxyServer.getInstance().getPlayers().values();
    }

    @Override
    protected void connect(@NotNull ProxiedPlayer player, @NotNull String service) {
        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(service);
        if (serverInfo != null) {
            player.connect(serverInfo);
        }
    }

    @Override
    protected void kick(@NotNull ProxiedPlayer player, @NotNull String reason) {
        player.disconnect(reason);
    }

    @Override
    protected void sendMessage(@NotNull ProxiedPlayer player, @NotNull String message) {
        player.sendMessage(message);
    }

    @Override
    protected void sendMessageComponent(@NotNull ProxiedPlayer player, @NotNull String data) {

    }

    @Override
    protected void sendPluginMessage(@NotNull ProxiedPlayer player, @NotNull String tag, @NotNull byte[] data) {
    }


    @Override
    protected void broadcastMessageComponent(@NotNull String data, @Nullable String permission) {

    }

    @Override
    protected void broadcastMessage(@NotNull String message, @Nullable String permission) {
        for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers().values()) {
            if (permission == null || proxiedPlayer.hasPermission(permission)) {
                proxiedPlayer.sendMessage(message);
            }
        }
    }

    @Override
    protected void connectToFallback(@NotNull ProxiedPlayer player) {
        WaterdogPECloudNetHelper.connectToFallback(player, player.getServer() != null ? player.getServer().getInfo().getServerName() : null);
    }

    @Override
    protected void dispatchCommand(@NotNull ProxiedPlayer player, @NotNull String command) {
        ProxyServer.getInstance().dispatchCommand(player, command);
    }

}
