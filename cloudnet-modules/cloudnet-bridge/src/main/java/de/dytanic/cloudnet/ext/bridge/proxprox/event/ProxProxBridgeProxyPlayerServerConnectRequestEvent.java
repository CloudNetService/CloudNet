package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;

public class ProxProxBridgeProxyPlayerServerConnectRequestEvent extends ProxProxBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    private final NetworkServiceInfo networkServiceInfo;

    public ProxProxBridgeProxyPlayerServerConnectRequestEvent(NetworkConnectionInfo networkConnectionInfo, NetworkServiceInfo networkServiceInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkServiceInfo = networkServiceInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }

    public NetworkServiceInfo getNetworkServiceInfo() {
        return this.networkServiceInfo;
    }
}