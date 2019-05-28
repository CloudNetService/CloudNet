package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BungeeNetworkClusterNodeInfoUpdateEvent extends
    BungeeCloudNetEvent {

  @Getter
  private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;
}