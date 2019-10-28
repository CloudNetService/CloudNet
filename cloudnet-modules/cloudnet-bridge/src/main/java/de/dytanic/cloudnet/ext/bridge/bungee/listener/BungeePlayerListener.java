package de.dytanic.cloudnet.ext.bridge.bungee.listener;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeePlayerListener implements Listener {

    @EventHandler
    public void handle(LoginEvent event) {
        BridgeHelper.sendChannelMessageProxyLoginRequest(BungeeCloudNetHelper.createNetworkConnectionInfo(event.getConnection()));
    }

    @EventHandler
    public void handle(PostLoginEvent event) {
        BridgeHelper.sendChannelMessageProxyLoginSuccess(BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()));
        BridgeHelper.updateServiceInfo();
    }

    @EventHandler
    public void handle(ServerSwitchEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(event.getPlayer().getServer().getInfo().getName());

        if (serviceInfoSnapshot != null) {
            BridgeHelper.sendChannelMessageProxyServerSwitch(BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()),
                    new NetworkServiceInfo(serviceInfoSnapshot.getServiceId().getEnvironment(), serviceInfoSnapshot.getServiceId().getUniqueId(),
                            serviceInfoSnapshot.getServiceId().getName()));
        }
    }

    @EventHandler
    public void handle(ServerConnectEvent event) {
        ProxiedPlayer proxiedPlayer = event.getPlayer();

        ServiceInfoSnapshot serviceInfoSnapshot = BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(event.getTarget().getName());

        if (serviceInfoSnapshot != null) {
            BridgeHelper.sendChannelMessageProxyServerConnectRequest(BungeeCloudNetHelper.createNetworkConnectionInfo(proxiedPlayer.getPendingConnection()),
                    new NetworkServiceInfo(serviceInfoSnapshot.getServiceId().getEnvironment(), serviceInfoSnapshot.getServiceId().getUniqueId(),
                            serviceInfoSnapshot.getServiceId().getName()));

            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    @EventHandler
    public void handle(ServerKickEvent event) {
        Server previousServer = event.getPlayer().getServer();
        if (previousServer == null || BungeeCloudNetHelper.isFallbackServer(previousServer.getInfo())) {
            event.getPlayer().disconnect(event.getKickReasonComponent());
            event.setCancelled(true);
            return;
        }
        String server = BungeeCloudNetHelper.filterServiceForProxiedPlayer(event.getPlayer(), event.getPlayer().getServer() != null ? event.getPlayer().getServer().getInfo().getName() : null);

        if (server != null && ProxyServer.getInstance().getServers().containsKey(server)) {
            event.setCancelled(true);
            event.setCancelServer(ProxyServer.getInstance().getServerInfo(server));
            event.getPlayer().sendMessage(event.getKickReasonComponent());
        }
    }

    @EventHandler
    public void handle(PlayerDisconnectEvent event) {
        BridgeHelper.sendChannelMessageProxyDisconnect(BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()));

        Wrapper.getInstance().runTask(BridgeHelper::updateServiceInfo);
    }
}