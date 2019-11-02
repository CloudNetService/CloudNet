package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;

public final class ProxProxBridgeServerPlayerLoginSuccessEvent extends ProxProxBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    private final NetworkPlayerServerInfo networkPlayerServerInfo;

    public ProxProxBridgeServerPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkPlayerServerInfo = networkPlayerServerInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }

    public NetworkPlayerServerInfo getNetworkPlayerServerInfo() {
        return this.networkPlayerServerInfo;
    }
}