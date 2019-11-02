package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;

public final class ProxProxNetworkClusterNodeInfoUpdateEvent extends ProxProxCloudNetEvent {

    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

    public ProxProxNetworkClusterNodeInfoUpdateEvent(NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
        this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
    }

    public NetworkClusterNodeInfoSnapshot getNetworkClusterNodeInfoSnapshot() {
        return this.networkClusterNodeInfoSnapshot;
    }
}