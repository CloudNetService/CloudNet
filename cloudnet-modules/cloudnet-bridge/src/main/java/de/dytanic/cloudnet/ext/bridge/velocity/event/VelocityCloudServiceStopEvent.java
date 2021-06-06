package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class VelocityCloudServiceStopEvent extends VelocityCloudNetEvent {

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  public VelocityCloudServiceStopEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }
}
