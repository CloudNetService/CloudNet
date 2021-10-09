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

import aerogel.Inject;
import aerogel.Name;
import aerogel.Singleton;
import aerogel.auto.Provides;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.ServiceConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.JVMService;
import de.dytanic.cloudnet.util.PortValidator;
import org.jetbrains.annotations.NotNull;

@Singleton
@Name("JVMServiceFactory")
@Provides(ICloudServiceFactory.class)
public class JVMServiceFactory implements ICloudServiceFactory {

  private final CloudNet nodeInstance;
  private final IEventManager eventManager;

  @Inject
  public JVMServiceFactory(CloudNet nodeInstance, IEventManager eventManager) {
    this.nodeInstance = nodeInstance;
    this.eventManager = eventManager;
  }

  @Override
  public @NotNull ICloudService createCloudService(
    @NotNull ICloudServiceManager manager,
    @NotNull ServiceConfiguration configuration
  ) {
    // select the configuration preparer for the environment
    ServiceConfigurationPreparer preparer = manager
      .getServicePreparer(configuration.getProcessConfig().getEnvironment())
      .orElseThrow(() -> new IllegalArgumentException("Unable to prepare config for " + configuration.getServiceId()));
    // find a free port for the service
    int port = configuration.getPort();
    while (this.isPortInUse(manager, port)) {
      port++;
    }
    // set the port
    configuration.setPort(port);
    // create the service
    return new JVMService(configuration, manager, this.eventManager, this.nodeInstance, preparer);
  }

  protected boolean isPortInUse(@NotNull ICloudServiceManager manager, int port) {
    // check if any local service has the port
    for (ICloudService cloudService : manager.getLocalCloudServices()) {
      if (cloudService.getServiceConfiguration().getPort() == port) {
        return true;
      }
    }
    // validate that the port is free
    return !PortValidator.checkPort(port);
  }
}
