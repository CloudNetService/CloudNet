package de.dytanic.cloudnet.ext.bridge.proxprox.listener;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.proxprox.ProxProxCloudNetHelper;
import io.gomint.proxprox.api.data.ServerDataHolder;
import io.gomint.proxprox.api.event.*;
import io.gomint.proxprox.api.plugin.event.EventHandler;
import io.gomint.proxprox.api.plugin.event.Listener;

public final class ProxProxPlayerListener implements Listener {

    @EventHandler
    public void handle(PlayerLoggedinEvent event) {
        BridgeHelper.sendChannelMessageProxyLoginRequest(ProxProxCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));
    }

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        BridgeHelper.sendChannelMessageProxyLoginSuccess(ProxProxCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));
        BridgeHelper.updateServiceInfo();
    }

    @EventHandler
    public void handle(PlayerSwitchEvent event) {
        if (event.getFrom() == null) {
            String server = ProxProxCloudNetHelper.filterServiceForPlayer(event.getPlayer(), null);

            if (server != null && ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.containsKey(server)) {
                ServiceInfoSnapshot serviceInfoSnapshot = ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(server);
                event.setTo(new ServerDataHolder(serviceInfoSnapshot.getAddress().getHost(), serviceInfoSnapshot.getAddress().getPort()));
            }
        }

        ServiceInfoSnapshot serviceInfoSnapshot = ProxProxCloudNetHelper.getServiceInfoSnapshotByHostAndPort(event.getTo().getIP(), event.getTo().getPort());

        if (serviceInfoSnapshot != null) {
            BridgeHelper.sendChannelMessageProxyServerConnectRequest(ProxProxCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                    new NetworkServiceInfo(serviceInfoSnapshot.getServiceId().getEnvironment(), serviceInfoSnapshot.getServiceId().getUniqueId(),
                            serviceInfoSnapshot.getServiceId().getName()));
        }
    }

    @EventHandler
    public void handle(PlayerSwitchedEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = ProxProxCloudNetHelper.getServiceInfoSnapshotByHostAndPort(event.getTo().getIP(), event.getTo().getPort());

        if (serviceInfoSnapshot != null) {
            BridgeHelper.sendChannelMessageProxyServerSwitch(ProxProxCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                    new NetworkServiceInfo(serviceInfoSnapshot.getServiceId().getEnvironment(), serviceInfoSnapshot.getServiceId().getUniqueId(),
                            serviceInfoSnapshot.getServiceId().getName()));
        }
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        BridgeHelper.sendChannelMessageProxyDisconnect(ProxProxCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));

        BridgeHelper.updateServiceInfo();
    }
}