package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import org.bukkit.event.HandlerList;

public final class BukkitNetworkClusterNodeInfoUpdateEvent extends BukkitCloudNetEvent {

    private static HandlerList handlerList = new HandlerList();

    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

    public BukkitNetworkClusterNodeInfoUpdateEvent(NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
        this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
    }

    public static HandlerList getHandlerList() {
        return BukkitNetworkClusterNodeInfoUpdateEvent.handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public NetworkClusterNodeInfoSnapshot getNetworkClusterNodeInfoSnapshot() {
        return this.networkClusterNodeInfoSnapshot;
    }
}