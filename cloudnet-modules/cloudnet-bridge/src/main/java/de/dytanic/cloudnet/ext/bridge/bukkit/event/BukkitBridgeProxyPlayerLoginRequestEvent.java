package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class BukkitBridgeProxyPlayerLoginRequestEvent extends BukkitBridgeEvent {

    private static final HandlerList handlerList = new HandlerList();
    private final NetworkConnectionInfo networkConnectionInfo;

    public BukkitBridgeProxyPlayerLoginRequestEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public static HandlerList getHandlerList() {
        return BukkitBridgeProxyPlayerLoginRequestEvent.handlerList;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }
}