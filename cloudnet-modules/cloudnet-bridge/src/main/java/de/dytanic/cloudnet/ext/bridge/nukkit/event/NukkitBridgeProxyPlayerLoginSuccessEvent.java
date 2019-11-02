package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;

public final class NukkitBridgeProxyPlayerLoginSuccessEvent extends NukkitBridgeEvent {

    private static final HandlerList handlers = new HandlerList();

    private final NetworkConnectionInfo networkConnectionInfo;

    public NukkitBridgeProxyPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public static HandlerList getHandlers() {
        return NukkitBridgeProxyPlayerLoginSuccessEvent.handlers;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}