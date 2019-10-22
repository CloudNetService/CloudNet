package de.dytanic.cloudnet.ext.bridge.bungee;
/*
 * Created by derrop on 19.10.2019
 */

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeeCloudNetReconnectHandler implements ReconnectHandler {
    @Override
    public ServerInfo getServer(ProxiedPlayer player) {
        String server = BungeeCloudNetHelper.filterServiceForProxiedPlayer(player, player.getServer() != null ? player.getServer().getInfo().getName() : null);
        return server != null ? ProxyServer.getInstance().getServerInfo(server) : null;
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
