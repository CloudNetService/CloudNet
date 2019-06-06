package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class VelocityBridgeProxyPlayerDisconnectEvent extends VelocityBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    public VelocityBridgeProxyPlayerDisconnectEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}