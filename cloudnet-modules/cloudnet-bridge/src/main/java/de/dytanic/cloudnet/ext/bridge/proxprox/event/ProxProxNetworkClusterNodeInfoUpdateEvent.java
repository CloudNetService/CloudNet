package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ProxProxNetworkClusterNodeInfoUpdateEvent extends
    ProxProxCloudNetEvent {

  @Getter
  private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;
}