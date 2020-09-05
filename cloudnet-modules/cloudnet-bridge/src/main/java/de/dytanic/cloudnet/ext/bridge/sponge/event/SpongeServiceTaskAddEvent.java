package de.dytanic.cloudnet.ext.bridge.sponge.event;


import de.dytanic.cloudnet.driver.service.ServiceTask;

public class SpongeServiceTaskAddEvent extends SpongeCloudNetEvent {

    private final ServiceTask task;

    public SpongeServiceTaskAddEvent(ServiceTask task) {
        this.task = task;
    }

    public ServiceTask getTask() {
        return task;
    }

}
