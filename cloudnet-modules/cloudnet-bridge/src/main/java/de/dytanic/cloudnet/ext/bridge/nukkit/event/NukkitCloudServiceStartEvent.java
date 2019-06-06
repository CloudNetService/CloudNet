package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class NukkitCloudServiceStartEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    public NukkitCloudServiceStartEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public static HandlerList getHandlers() {
        return NukkitCloudServiceStartEvent.handlers;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }
}