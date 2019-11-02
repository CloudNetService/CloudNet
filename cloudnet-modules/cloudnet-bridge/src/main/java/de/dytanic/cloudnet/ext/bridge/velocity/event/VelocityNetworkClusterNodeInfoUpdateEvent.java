package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;

public final class VelocityNetworkClusterNodeInfoUpdateEvent extends VelocityCloudNetEvent {

    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

    public VelocityNetworkClusterNodeInfoUpdateEvent(NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
        this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
    }

    public NetworkClusterNodeInfoSnapshot getNetworkClusterNodeInfoSnapshot() {
        return this.networkClusterNodeInfoSnapshot;
    }
}