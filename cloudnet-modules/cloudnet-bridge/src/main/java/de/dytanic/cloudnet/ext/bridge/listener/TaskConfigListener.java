package de.dytanic.cloudnet.ext.bridge.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.task.ServiceTaskAddEvent;

public class TaskConfigListener {

  @EventListener
  public void handleTaskAdd(ServiceTaskAddEvent event) {
    ServiceTask serviceTask = event.getTask();
    if (serviceTask.getProcessConfiguration().getEnvironment().isMinecraftServer() && !serviceTask.getProperties()
      .contains("requiredPermission")) {
      serviceTask.getProperties().appendNull("requiredPermission");
      CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
    }
  }

}
