package de.dytanic.cloudnet.event.service.task;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.service.ServiceTask;

public class LocalServiceTaskRemoveEvent extends Event implements ICancelable {

    private final ServiceTask task;

    private boolean cancelled;

    public LocalServiceTaskRemoveEvent(ServiceTask task) {
        this.task = task;
    }

    public ServiceTask getTask() {
        return this.task;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
