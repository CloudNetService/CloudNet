package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class NukkitServiceInfoSnapshotConfigureEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    public NukkitServiceInfoSnapshotConfigureEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public static HandlerList getHandlers() {
        return NukkitServiceInfoSnapshotConfigureEvent.handlers;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }
}