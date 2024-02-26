/*
 * Copyright 2019-2024 CloudNetService team & contributors
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
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.modules.docker.config.DockerConfiguration;
import eu.cloudnetservice.node.TickLoop;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.defaults.factory.BaseLocalCloudServiceFactory;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class DockerizedLocalCloudServiceFactory extends BaseLocalCloudServiceFactory {

  protected final TickLoop mainThread;
  protected final EventManager eventManager;
  protected final DockerClient dockerClient;
  protected final ServiceTaskProvider taskProvider;
  protected final DockerConfiguration dockerConfiguration;
  protected final CloudServiceManager cloudServiceManager;

  @Inject
  public DockerizedLocalCloudServiceFactory(
    @NonNull TickLoop tickLoop,
    @NonNull Configuration nodeConfig,
    @NonNull CloudServiceManager cloudServiceManager,
    @NonNull EventManager eventManager,
    @NonNull ServiceVersionProvider versionProvider,
    @NonNull DockerClient dockerClient,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull DockerConfiguration configuration
  ) {
    super(nodeConfig, versionProvider);
    this.mainThread = tickLoop;
    this.eventManager = eventManager;
    this.cloudServiceManager = cloudServiceManager;
    this.dockerClient = dockerClient;
    this.taskProvider = taskProvider;
    this.dockerConfiguration = configuration;
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
    return new DockerizedService(
      this.mainThread,
      this.configuration,
      this.taskProvider,
      config,
      manager,
      this.eventManager,
      this.versionProvider,
      preparer,
      this.dockerClient,
      this.dockerConfiguration);
  }

  @Override
  public @NonNull String name() {
    return this.dockerConfiguration.factoryName();
  }
}
