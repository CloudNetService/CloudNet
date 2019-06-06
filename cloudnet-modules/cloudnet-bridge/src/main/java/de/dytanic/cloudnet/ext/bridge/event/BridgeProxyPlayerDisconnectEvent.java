package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class BridgeProxyPlayerDisconnectEvent extends DriverEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    public BridgeProxyPlayerDisconnectEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}