package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class GoMintBridgeProxyPlayerLoginSuccessEvent extends GoMintBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    public GoMintBridgeProxyPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}