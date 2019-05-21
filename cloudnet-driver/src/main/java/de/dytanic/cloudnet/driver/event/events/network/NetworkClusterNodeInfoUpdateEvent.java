package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;

@Getter
public final class NetworkClusterNodeInfoUpdateEvent extends NetworkEvent {

    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

    public NetworkClusterNodeInfoUpdateEvent(INetworkChannel channel, NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot)
    {
        super(channel);

        this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
    }
}