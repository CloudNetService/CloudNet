package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * {@inheritDoc}
 */
public final class NukkitCloudServiceUnregisterEvent extends NukkitCloudNetEvent {

  private static final HandlerList handlers = new HandlerList();

  private final ServiceInfoSnapshot serviceInfoSnapshot;

  public NukkitCloudServiceUnregisterEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  public static HandlerList getHandlers() {
    return NukkitCloudServiceUnregisterEvent.handlers;
  }

  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.serviceInfoSnapshot;
  }
}
