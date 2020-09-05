package de.dytanic.cloudnet.ext.bridge.bukkit.event;


import de.dytanic.cloudnet.driver.service.ServiceTask;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BukkitServiceTaskAddEvent extends BukkitCloudNetEvent {

    private static final HandlerList handlerList = new HandlerList();

    private final ServiceTask task;

    public BukkitServiceTaskAddEvent(ServiceTask task) {
        this.task = task;
    }

    public static HandlerList getHandlerList() {
        return BukkitServiceTaskAddEvent.handlerList;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public ServiceTask getTask() {
        return task;
    }

}
