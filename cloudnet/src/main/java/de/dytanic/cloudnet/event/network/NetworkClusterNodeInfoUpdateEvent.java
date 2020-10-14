package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;

public final class NetworkClusterNodeInfoUpdateEvent extends NetworkEvent {

    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

    public NetworkClusterNodeInfoUpdateEvent(INetworkChannel channel, NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
        super(channel);

        this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
    }

    public NetworkClusterNodeInfoSnapshot getNetworkClusterNodeInfoSnapshot() {
        return this.networkClusterNodeInfoSnapshot;
    }
}