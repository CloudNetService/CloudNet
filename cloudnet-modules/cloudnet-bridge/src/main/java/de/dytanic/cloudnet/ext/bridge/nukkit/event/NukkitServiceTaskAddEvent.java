package de.dytanic.cloudnet.ext.bridge.nukkit.event;


import de.dytanic.cloudnet.driver.service.ServiceTask;
import org.bukkit.event.HandlerList;

public class NukkitServiceTaskAddEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final ServiceTask task;

    public NukkitServiceTaskAddEvent(ServiceTask task) {
        this.task = task;
    }

    public static HandlerList getHandlers() {
        return NukkitServiceTaskAddEvent.handlers;
    }

    public ServiceTask getTask() {
        return task;
    }

}
