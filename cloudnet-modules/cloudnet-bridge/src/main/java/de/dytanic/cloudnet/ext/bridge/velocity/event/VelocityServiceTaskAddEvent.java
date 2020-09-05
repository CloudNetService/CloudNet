package de.dytanic.cloudnet.ext.bridge.velocity.event;


import de.dytanic.cloudnet.driver.service.ServiceTask;

public class VelocityServiceTaskAddEvent extends VelocityCloudNetEvent {

    private final ServiceTask task;

    public VelocityServiceTaskAddEvent(ServiceTask task) {
        this.task = task;
    }

    public ServiceTask getTask() {
        return task;
    }

}
