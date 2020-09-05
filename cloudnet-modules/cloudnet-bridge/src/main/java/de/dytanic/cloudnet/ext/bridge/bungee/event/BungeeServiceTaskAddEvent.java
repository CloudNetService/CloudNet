package de.dytanic.cloudnet.ext.bridge.bungee.event;


import de.dytanic.cloudnet.driver.service.ServiceTask;

public class BungeeServiceTaskAddEvent extends BungeeCloudNetEvent {

    private final ServiceTask task;

    public BungeeServiceTaskAddEvent(ServiceTask task) {
        this.task = task;
    }

    public ServiceTask getTask() {
        return task;
    }

}
