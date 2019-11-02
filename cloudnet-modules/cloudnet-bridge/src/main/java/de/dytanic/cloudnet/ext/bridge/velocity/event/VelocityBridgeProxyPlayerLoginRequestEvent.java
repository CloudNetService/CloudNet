package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class VelocityBridgeProxyPlayerLoginRequestEvent extends VelocityBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    public VelocityBridgeProxyPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}