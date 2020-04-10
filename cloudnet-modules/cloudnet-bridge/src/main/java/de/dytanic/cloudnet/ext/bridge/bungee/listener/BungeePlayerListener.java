package de.dytanic.cloudnet.ext.bridge.bungee.listener;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

public final class BungeePlayerListener implements Listener {

    private BungeeCloudNetBridgePlugin plugin;

    public BungeePlayerListener(BungeeCloudNetBridgePlugin plugin) {
        this.plugin = plugin;
    }

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
        ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper.getCachedServiceInfoSnapshot(event.getPlayer().getServer().getInfo().getName());

        if (serviceInfoSnapshot != null) {
            BridgeHelper.sendChannelMessageProxyServerSwitch(BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()),
                    new NetworkServiceInfo(
                            serviceInfoSnapshot.getServiceId().getEnvironment(),
                            Wrapper.getInstance().getServiceId(),
                            Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
                    )
            );
        }
    }

    @EventHandler
    public void handle(ServerConnectEvent event) {
        ProxiedPlayer proxiedPlayer = event.getPlayer();

        ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper.getCachedServiceInfoSnapshot(event.getTarget().getName());

        if (serviceInfoSnapshot != null) {
            BridgeHelper.sendChannelMessageProxyServerConnectRequest(BungeeCloudNetHelper.createNetworkConnectionInfo(proxiedPlayer.getPendingConnection()),
                    new NetworkServiceInfo(
                            serviceInfoSnapshot.getServiceId().getEnvironment(),
                            Wrapper.getInstance().getServiceId(),
                            Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
                    )
            );

            try {
                Thread.sleep(150);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    @EventHandler
    public void handle(ServerKickEvent event) {
        ServerInfo kickFrom = event.getKickedFrom();

        if (kickFrom == null) {
            event.getPlayer().disconnect(event.getKickReasonComponent());
            event.setCancelled(true);
            return;
        }
        BridgeProxyHelper.handleConnectionFailed(event.getPlayer().getUniqueId(), kickFrom.getName());

        BungeeCloudNetHelper.getNextFallback(event.getPlayer()).ifPresent(serverInfo -> {
            event.setCancelled(true);
            event.setCancelServer(serverInfo);
            event.getPlayer().sendMessage(event.getKickReasonComponent());
        });
    }

    @EventHandler
    public void handle(PlayerDisconnectEvent event) {
        BridgeHelper.sendChannelMessageProxyDisconnect(BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()));
        BridgeProxyHelper.clearFallbackProfile(event.getPlayer().getUniqueId());

        ProxyServer.getInstance().getScheduler().schedule(this.plugin, BridgeHelper::updateServiceInfo, 50, TimeUnit.MILLISECONDS);
    }
}