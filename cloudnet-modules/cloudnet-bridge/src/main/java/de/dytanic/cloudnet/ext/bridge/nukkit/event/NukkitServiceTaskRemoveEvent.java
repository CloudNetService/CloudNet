package de.dytanic.cloudnet.ext.bridge.nukkit.event;


import de.dytanic.cloudnet.driver.service.ServiceTask;
import org.bukkit.event.HandlerList;

public class NukkitServiceTaskRemoveEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final ServiceTask task;

    public NukkitServiceTaskRemoveEvent(ServiceTask task) {
        this.task = task;
    }

    public static HandlerList getHandlers() {
        return NukkitServiceTaskRemoveEvent.handlers;
    }

    public ServiceTask getTask() {
        return task;
    }

}
