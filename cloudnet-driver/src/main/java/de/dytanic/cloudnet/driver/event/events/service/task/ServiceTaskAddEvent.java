package de.dytanic.cloudnet.driver.event.events.service.task;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceTask;

public final class ServiceTaskAddEvent extends Event {

    private final ServiceTask task;

    public ServiceTaskAddEvent(ServiceTask task) {
        this.task = task;
    }

    public ServiceTask getTask() {
        return this.task;
    }

}