package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * {@inheritDoc}
 */
public final class GoMintCloudServiceStopEvent extends GoMintCloudNetEvent {

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  public GoMintCloudServiceStopEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }
}
