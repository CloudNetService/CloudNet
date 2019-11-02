package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;

public final class SpongeNetworkClusterNodeInfoUpdateEvent extends SpongeCloudNetEvent {

    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

    public SpongeNetworkClusterNodeInfoUpdateEvent(NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
        this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
    }

    public NetworkClusterNodeInfoSnapshot getNetworkClusterNodeInfoSnapshot() {
        return this.networkClusterNodeInfoSnapshot;
    }
}