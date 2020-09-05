package de.dytanic.cloudnet.ext.bridge.bungee.event;


import de.dytanic.cloudnet.driver.service.ServiceTask;

public class BungeeServiceTaskRemoveEvent extends BungeeCloudNetEvent {

    private final ServiceTask task;

    public BungeeServiceTaskRemoveEvent(ServiceTask task) {
        this.task = task;
    }

    public ServiceTask getTask() {
        return task;
    }

}
