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

package de.dytanic.cloudnet.service.defaults.factory;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.util.PortValidator;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractServiceFactory implements ICloudServiceFactory {

  protected void validateConfiguration(
    @NotNull ICloudServiceManager manager,
    @NotNull ServiceConfiguration configuration
  ) {
    // set the node unique id
    configuration.serviceId().nodeUniqueId(CloudNet.getInstance().nodeUniqueId());

    // set the environment type
    if (configuration.serviceId().environment() == null) {
      var env = CloudNet.getInstance().serviceVersionProvider()
        .getEnvironmentType(configuration.serviceId().environmentName())
        .orElseThrow(() -> new IllegalArgumentException(
          "Unknown environment type " + configuration.serviceId().environmentName()));
      // set the environment type
      configuration.serviceId().environment(env);
    }

    // find a free port for the service
    var port = configuration.port();
    while (this.isPortInUse(manager, port)) {
      port++;
    }
    // set the port
    configuration.port(port);
  }

  protected boolean isPortInUse(@NotNull ICloudServiceManager manager, int port) {
    // check if any local service has the port
    for (var cloudService : manager.localCloudService()) {
      if (cloudService.getServiceConfiguration().port() == port) {
        return true;
      }
    }
    // validate that the port is free
    return !PortValidator.checkPort(port);
  }
}
