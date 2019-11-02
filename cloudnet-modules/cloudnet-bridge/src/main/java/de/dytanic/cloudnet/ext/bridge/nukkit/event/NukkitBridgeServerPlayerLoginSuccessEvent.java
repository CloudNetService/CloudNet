package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;

public final class NukkitBridgeServerPlayerLoginSuccessEvent extends NukkitBridgeEvent {

    private static final HandlerList handlers = new HandlerList();

    private final NetworkConnectionInfo networkConnectionInfo;

    private final NetworkPlayerServerInfo networkPlayerServerInfo;

    public NukkitBridgeServerPlayerLoginSuccessEvent(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkPlayerServerInfo = networkPlayerServerInfo;
    }

    public static HandlerList getHandlers() {
        return NukkitBridgeServerPlayerLoginSuccessEvent.handlers;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }

    public NetworkPlayerServerInfo getNetworkPlayerServerInfo() {
        return this.networkPlayerServerInfo;
    }
}