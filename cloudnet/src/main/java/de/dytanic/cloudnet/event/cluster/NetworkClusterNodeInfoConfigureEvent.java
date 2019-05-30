package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class NetworkClusterNodeInfoConfigureEvent extends DriverEvent {

  private final NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot;

}