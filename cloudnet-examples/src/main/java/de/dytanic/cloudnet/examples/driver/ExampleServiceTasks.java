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

package de.dytanic.cloudnet.examples.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Arrays;
import java.util.Collections;

public final class ExampleServiceTasks {

  public void test() {
    this.addServiceTask();
    this.updateServiceTask();
    this.removeServiceTask();
  }

  public void addServiceTask() {

    ServiceTask serviceTask = ServiceTask.builder()
      .includes(
        Collections.singletonList(new ServiceRemoteInclusion("https://cloudnetservice.eu", "destination"))) //includes
      .templates(Collections.singletonList(new ServiceTemplate("Global", "default", "local", true))) //templates
      .deployments(Collections.singletonList(
        new ServiceDeployment(new ServiceTemplate("Backup", "Global", "local", true),
          Arrays.asList("some", "excluded", "files")))) //deployments
      .name("Lobby") //name
      .runtime("jvm") //runtime can be null for the default jvm wrapper or "jvm"
      .maintenance(true) //maintenance
      .autoDeleteOnStop(true) //autoDeleteOnStop => if the service stops naturally it will be automatic deleted
      .staticServices(
        true) //The service won't be deleted fully and will store in the configured directory. The default is /local/services
      .associatedNodes(Arrays.asList("Node-1", "Node-2")) //node ids
      .groups(Arrays.asList("Global", "Lobby", "Global-Server")) //groups
      .serviceEnvironmentType(ServiceEnvironmentType.MINECRAFT_SERVER) //environement type
      .maxHeapMemory(1234) //max heap memory size
      .startPort(25565) //start port
      .minServiceCount(187) //min services count with auto creation
      .build();

    CloudNetDriver.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
  }

  public void updateServiceTask() {
    if (CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent("TestTask")) {
      CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTaskAsync("TestTask").onComplete(result -> {
        result.setMinServiceCount(1);
        CloudNetDriver.getInstance().getServiceTaskProvider().addPermanentServiceTask(result);
      });
    }
  }

  public void removeServiceTask() {
    CloudNetDriver.getInstance().getServiceTaskProvider().removePermanentServiceTask("TestTask");
  }
}
