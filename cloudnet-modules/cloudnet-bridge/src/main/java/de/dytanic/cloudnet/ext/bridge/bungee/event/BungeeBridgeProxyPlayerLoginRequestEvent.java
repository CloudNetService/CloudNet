package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class BungeeBridgeProxyPlayerLoginRequestEvent extends BungeeBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    public BungeeBridgeProxyPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}