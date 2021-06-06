package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * {@inheritDoc}
 */
public final class GoMintCloudServiceRegisterEvent extends GoMintCloudNetEvent {

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  public GoMintCloudServiceRegisterEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }
}
