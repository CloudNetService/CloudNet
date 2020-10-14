package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class GoMintBridgeProxyPlayerDisconnectEvent extends GoMintBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    public GoMintBridgeProxyPlayerDisconnectEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}