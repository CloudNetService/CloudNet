package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class ProxProxBridgeProxyPlayerLoginSuccessEvent extends ProxProxBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    public ProxProxBridgeProxyPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}