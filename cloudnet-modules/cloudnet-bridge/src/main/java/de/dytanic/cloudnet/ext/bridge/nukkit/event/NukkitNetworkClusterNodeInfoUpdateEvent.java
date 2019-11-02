package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;

public final class NukkitNetworkClusterNodeInfoUpdateEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

    public NukkitNetworkClusterNodeInfoUpdateEvent(NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
        this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
    }

    public static HandlerList getHandlers() {
        return NukkitNetworkClusterNodeInfoUpdateEvent.handlers;
    }

    public NetworkClusterNodeInfoSnapshot getNetworkClusterNodeInfoSnapshot() {
        return this.networkClusterNodeInfoSnapshot;
    }
}