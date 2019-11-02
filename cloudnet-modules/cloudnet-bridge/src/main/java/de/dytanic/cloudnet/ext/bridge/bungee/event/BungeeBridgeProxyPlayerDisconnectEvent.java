package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class BungeeBridgeProxyPlayerDisconnectEvent extends BungeeBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    public BungeeBridgeProxyPlayerDisconnectEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}