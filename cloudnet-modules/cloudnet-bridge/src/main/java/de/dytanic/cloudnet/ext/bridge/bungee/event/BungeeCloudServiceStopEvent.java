package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class BungeeCloudServiceStopEvent extends BungeeCloudNetEvent {

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  public BungeeCloudServiceStopEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }
}
