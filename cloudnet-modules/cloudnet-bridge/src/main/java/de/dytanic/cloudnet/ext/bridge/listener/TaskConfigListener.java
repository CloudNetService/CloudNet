/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
