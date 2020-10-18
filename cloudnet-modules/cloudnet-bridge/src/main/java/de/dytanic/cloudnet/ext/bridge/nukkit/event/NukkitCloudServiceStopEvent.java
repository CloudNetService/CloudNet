package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * {@inheritDoc}
 */
public final class NukkitCloudServiceStopEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    public NukkitCloudServiceStopEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public static HandlerList getHandlers() {
        return NukkitCloudServiceStopEvent.handlers;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }
}