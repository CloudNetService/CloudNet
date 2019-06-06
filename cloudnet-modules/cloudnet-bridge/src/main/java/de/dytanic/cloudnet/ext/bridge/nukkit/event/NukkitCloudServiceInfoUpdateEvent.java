package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class NukkitCloudServiceInfoUpdateEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    public NukkitCloudServiceInfoUpdateEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public static HandlerList getHandlers() {
        return NukkitCloudServiceInfoUpdateEvent.handlers;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }
}