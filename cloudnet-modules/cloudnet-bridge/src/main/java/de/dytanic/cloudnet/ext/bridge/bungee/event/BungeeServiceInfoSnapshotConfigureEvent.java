package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class BungeeServiceInfoSnapshotConfigureEvent extends BungeeCloudNetEvent {

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  public BungeeServiceInfoSnapshotConfigureEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }
}
