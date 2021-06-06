package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class BungeeCloudServiceInfoUpdateEvent extends BungeeCloudNetEvent {

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  public BungeeCloudServiceInfoUpdateEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }
}
