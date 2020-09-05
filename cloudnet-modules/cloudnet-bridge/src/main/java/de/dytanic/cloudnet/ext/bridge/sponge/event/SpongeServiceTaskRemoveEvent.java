package de.dytanic.cloudnet.ext.bridge.sponge.event;


import de.dytanic.cloudnet.driver.service.ServiceTask;

public class SpongeServiceTaskRemoveEvent extends SpongeCloudNetEvent {

    private final ServiceTask task;

    public SpongeServiceTaskRemoveEvent(ServiceTask task) {
        this.task = task;
    }

    public ServiceTask getTask() {
        return task;
    }

}
