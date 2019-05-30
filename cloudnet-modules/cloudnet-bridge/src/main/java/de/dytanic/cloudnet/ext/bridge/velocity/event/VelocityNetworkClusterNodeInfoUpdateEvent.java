package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class VelocityNetworkClusterNodeInfoUpdateEvent extends
  VelocityCloudNetEvent {

  @Getter
  private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;
}