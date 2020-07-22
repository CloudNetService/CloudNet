package de.dytanic.cloudnet.ext.bridge.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.service.task.ServiceTaskAddEvent;

public class TaskConfigListener {

    @EventListener
    public void handleTaskAdd(ServiceTaskAddEvent event) {
        if(!event.getTask().getProperties().contains("requiredPermission")) {
            event.getTask().getProperties().append("requiredPermission", "null");
        }
    }

}
