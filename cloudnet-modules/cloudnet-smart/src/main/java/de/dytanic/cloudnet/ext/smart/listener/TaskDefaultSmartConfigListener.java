package de.dytanic.cloudnet.ext.smart.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.service.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.ext.smart.CloudNetSmartModule;

public class TaskDefaultSmartConfigListener {

    @EventListener
    public void handleTaskAdd(ServiceTaskAddEvent event) {
        if (!event.getTask().getProperties().contains(CloudNetSmartModule.SMART_CONFIG_ENTRY)) {
            event.getTask().getProperties().append(CloudNetSmartModule.SMART_CONFIG_ENTRY, CloudNetSmartModule.getInstance().createDefaultSmartServiceTaskConfig());
        }
    }

}
