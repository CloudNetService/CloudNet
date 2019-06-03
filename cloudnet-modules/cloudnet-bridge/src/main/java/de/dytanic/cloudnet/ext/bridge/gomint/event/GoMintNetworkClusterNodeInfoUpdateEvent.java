package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GoMintNetworkClusterNodeInfoUpdateEvent extends GoMintCloudNetEvent {

    @Getter
    private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;
}