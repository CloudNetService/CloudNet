package de.dytanic.cloudnet.event.service.task;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudServiceManager;

public class ServiceTaskRemoveEvent extends Event implements ICancelable {

    private final ICloudServiceManager cloudServiceManager;

    private final ServiceTask task;

    private boolean cancelled;

    public ServiceTaskRemoveEvent(ICloudServiceManager cloudServiceManager, ServiceTask task) {
        this.cloudServiceManager = cloudServiceManager;
        this.task = task;
    }

    public ICloudServiceManager getCloudServiceManager() {
        return this.cloudServiceManager;
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
