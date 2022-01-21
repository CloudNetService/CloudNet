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

package eu.cloudnetservice.modules.docker;

import com.github.dockerjava.api.DockerClient;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.service.ServiceConfiguration;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.service.CloudService;
import eu.cloudnetservice.cloudnet.node.service.CloudServiceManager;
import eu.cloudnetservice.cloudnet.node.service.defaults.factory.AbstractServiceFactory;
import eu.cloudnetservice.modules.docker.config.DockerConfiguration;
import lombok.NonNull;

public class DockerizedServiceFactory extends AbstractServiceFactory {

  protected final CloudNet nodeInstance;
  protected final EventManager eventManager;
  protected final DockerClient dockerClient;
  protected final DockerConfiguration configuration;

  public DockerizedServiceFactory(
    @NonNull CloudNet nodeInstance,
    @NonNull EventManager eventManager,
    @NonNull DockerClient dockerClient,
    @NonNull DockerConfiguration configuration
  ) {
    this.nodeInstance = nodeInstance;
    this.eventManager = eventManager;
    this.dockerClient = dockerClient;
    this.configuration = configuration;
  }

  @Override
  public @NonNull CloudService createCloudService(
    @NonNull CloudServiceManager manager,
    @NonNull ServiceConfiguration configuration
  ) {
    // validates the settings of the configuration
    var config = this.validateConfiguration(manager, configuration);
    // select the configuration preparer for the environment
    var preparer = manager
      .servicePreparer(config.serviceId().environment())
      .orElseThrow(() -> new IllegalArgumentException("Unable to prepare config for " + config.serviceId()));
    // create the service
    return new DockerizedService(
      config,
      manager,
      this.eventManager,
      this.nodeInstance,
      preparer,
      this.dockerClient,
      this.configuration);
  }
}
