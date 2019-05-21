package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SpongeNetworkClusterNodeInfoUpdateEvent extends SpongeCloudNetEvent {

    @Getter
    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;
}