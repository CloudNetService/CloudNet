package de.dytanic.cloudnet.ext.bridge.bungee.listener;

import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.listener.PlayerExecutorListener;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public class BungeePlayerExecutorListener extends PlayerExecutorListener<ProxiedPlayer> {
    @Nullable
    @Override
    protected ProxiedPlayer getPlayer(@NotNull UUID uniqueId) {
        return ProxyServer.getInstance().getPlayer(uniqueId);
    }

    @Override
    protected @NotNull Collection<ProxiedPlayer> getOnlinePlayers() {
        return ProxyServer.getInstance().getPlayers();
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
        player.disconnect(TextComponent.fromLegacyText(reason));
    }

    @Override
    protected void sendMessage(@NotNull ProxiedPlayer player, @NotNull String message) {
        player.sendMessage(TextComponent.fromLegacyText(message));
    }

    @Override
    protected void sendMessageComponent(@NotNull ProxiedPlayer player, @NotNull String data) {
        player.sendMessage(ComponentSerializer.parse(data));
    }

    @Override
    protected void sendPluginMessage(@NotNull ProxiedPlayer player, @NotNull String tag, @NotNull byte[] data) {
        if (!ProxyServer.getInstance().getChannels().contains(tag)) {
            ProxyServer.getInstance().registerChannel(tag);
        }

        player.sendData(tag, data);
    }

    private void broadcastMessage(@NotNull BaseComponent[] messages, @Nullable String permission) {
        for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers()) {
            if (permission == null || proxiedPlayer.hasPermission(permission)) {
                proxiedPlayer.sendMessage(messages);
            }
        }
    }

    @Override
    protected void broadcastMessageComponent(@NotNull String data, @Nullable String permission) {
        this.broadcastMessage(ComponentSerializer.parse(data), permission);
    }

    @Override
    protected void broadcastMessage(@NotNull String message, @Nullable String permission) {
        this.broadcastMessage(TextComponent.fromLegacyText(message), permission);
    }

    @Override
    protected void connectToFallback(@NotNull ProxiedPlayer player) {
        BungeeCloudNetHelper.connectToFallback(player, player.getServer() != null ? player.getServer().getInfo().getName() : null);
    }

    @Override
    protected void dispatchCommand(@NotNull ProxiedPlayer player, @NotNull String command) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand(player, command);
    }
}
