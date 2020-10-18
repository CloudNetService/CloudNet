package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;

/**
 * {@inheritDoc}
 */
public class NukkitBridgeProxyPlayerServerConnectRequestEvent extends NukkitBridgeEvent {

    private static final HandlerList handlers = new HandlerList();

    private final NetworkConnectionInfo networkConnectionInfo;

    private final NetworkServiceInfo networkServiceInfo;

    public NukkitBridgeProxyPlayerServerConnectRequestEvent(NetworkConnectionInfo networkConnectionInfo, NetworkServiceInfo networkServiceInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkServiceInfo = networkServiceInfo;
    }

    public static HandlerList getHandlers() {
        return NukkitBridgeProxyPlayerServerConnectRequestEvent.handlers;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }

    public NetworkServiceInfo getNetworkServiceInfo() {
        return this.networkServiceInfo;
    }
}