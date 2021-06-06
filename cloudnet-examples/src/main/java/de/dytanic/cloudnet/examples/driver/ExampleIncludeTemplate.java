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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public final class ExampleIncludeTemplate {

  public void exampleIncludeTemplates(UUID playerUniqueId, ServiceInfoSnapshot serviceInfoSnapshot) {
    //Add serviceTemplate to existing service
    serviceInfoSnapshot.provider().addServiceTemplate(new ServiceTemplate("Lobby", "test1", "local"));

    ServiceInfoSnapshot newService = ServiceConfiguration.builder()
      // use a temporary task (could be a static one too)
      .task("PS-" + playerUniqueId.toString())
      // the runtime of the service to run in, 'jvm' is the default one
      .runtime("jvm")
      // deletes the service and all associated files when it gets stopped
      .autoDeleteOnStop(true)
      // if the service should be static meaning it gets created in a separate folder and no files will be
      // deleted after stopping it
      .staticService(false)
      // the inclusions to include before the service starts
      .inclusions(new ArrayList<>())
      // the templates to load before the service starts
      .templates(new ArrayList<>(Collections.singletonList(new ServiceTemplate(
        "Lobby", "test1", "local"
      ))))
      // the deployments to add to the service to deploy them later
      .deployments(new ArrayList<>())
      // the group of the service, all templates, inclusions and deployments of the groups gets loaded too
      .groups(Collections.singletonList("PrivateServerGroup"))
      // sets the maximum heap memory the service is allowed to use
      .maxHeapMemory(256)
      // sets the jvm options of the service
      .jvmOptions(new ArrayList<>())
      // sets the process parameters of the service
      .processParameters(new ArrayList<>())
      // defines the environment the service is running in
      .environment(ServiceEnvironmentType.MINECRAFT_SERVER)
      // sets the properties of the service, they can for example hold useful information about the service
      // in this example the owner unique id of the private server
      .properties(JsonDocument.newDocument().append("owner", playerUniqueId))
      // the start port of the service, if this port is already in use the next free starting from the
      // provided one is used
      .startPort(44955)
      // builds the service configuration which is immutable but can be re-used to create services
      .build()
      // creates a new service based on the created service configuration
      .createNewService();
    if (newService != null) {
      // if the service was created: start it!
      newService.provider().start();
    }
  }
}
