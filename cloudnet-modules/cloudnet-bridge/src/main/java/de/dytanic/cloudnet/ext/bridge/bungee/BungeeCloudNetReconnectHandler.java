package de.dytanic.cloudnet.ext.bridge.bungee;

import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeCloudNetReconnectHandler implements ReconnectHandler {

    @Override
    public ServerInfo getServer(ProxiedPlayer player) {
        return BungeeCloudNetHelper.getNextFallback(player).orElse(null);
    }

    @Override
    public void setServer(ProxiedPlayer player) {
    }

    @Override
    public void save() {
    }

    @Override
    public void close() {
    }

}
