package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class SpongeCloudServiceDisconnectNetworkEvent extends SpongeCloudNetEvent {

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  public SpongeCloudServiceDisconnectNetworkEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }
}
