/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.node.service.defaults.factory;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.defaults.JVMService;
import lombok.NonNull;

public class JVMServiceFactory extends AbstractServiceFactory {

  private final Node nodeInstance;
  private final EventManager eventManager;

  public JVMServiceFactory(@NonNull Node nodeInstance, @NonNull EventManager eventManager) {
    this.nodeInstance = nodeInstance;
    this.eventManager = eventManager;
  }

  @Override
  public @NonNull CloudService createCloudService(
    @NonNull CloudServiceManager manager,
    @NonNull ServiceConfiguration configuration
  ) {
    // validates the settings of the configuration
    var config = this.validateConfiguration(manager, configuration);
    // select the configuration preparer for the environment
    var preparer = manager.servicePreparer(config.serviceId().environment());
    // create the service
    return new JVMService(config, manager, this.eventManager, this.nodeInstance, preparer);
  }

  @Override
  public @NonNull String name() {
    return "jvm";
  }
}
