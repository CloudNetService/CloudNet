package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;

public final class NetworkClusterNodeInfoConfigureEvent extends DriverEvent {

  private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

  public NetworkClusterNodeInfoConfigureEvent(NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
    this.networkClusterNodeInfoSnapshot = networkClusterNodeInfoSnapshot;
  }

  @Override
  public boolean isShowDebug() {
    return false;
  }

  public NetworkClusterNodeInfoSnapshot getNetworkClusterNodeInfoSnapshot() {
    return this.networkClusterNodeInfoSnapshot;
  }

}
