package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * {@inheritDoc}
 */
public final class BukkitBridgeProxyPlayerDisconnectEvent extends BukkitBridgeEvent {

    private static final HandlerList handlerList = new HandlerList();
    private final NetworkConnectionInfo networkConnectionInfo;

    public BukkitBridgeProxyPlayerDisconnectEvent(NetworkConnectionInfo networkConnectionInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
    }

    public static HandlerList getHandlerList() {
        return BukkitBridgeProxyPlayerDisconnectEvent.handlerList;
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