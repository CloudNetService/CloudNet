/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.docker.impl;

import com.github.dockerjava.api.DockerClient;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.modules.docker.config.DockerConfiguration;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.service.InternalCloudServiceManager;
import eu.cloudnetservice.node.impl.service.defaults.factory.BaseLocalCloudServiceFactory;
import eu.cloudnetservice.node.impl.tick.DefaultTickLoop;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import eu.cloudnetservice.node.impl.version.ServiceVersionProvider;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public class DockerizedLocalCloudServiceFactory extends BaseLocalCloudServiceFactory {

  protected final I18n i18n;
  protected final DefaultTickLoop mainThread;
  protected final EventManager eventManager;
  protected final DockerClient dockerClient;
  protected final ServiceTaskProvider serviceTaskProvider;
  protected final DockerConfiguration dockerConfiguration;
  protected final CloudServiceManager cloudServiceManager;

  @Inject
  public DockerizedLocalCloudServiceFactory(
    @NonNull @Service I18n i18n,
    @NonNull DefaultTickLoop tickLoop,
    @NonNull Configuration nodeConfig,
    @NonNull CloudServiceManager cloudServiceManager,
    @NonNull EventManager eventManager,
    @NonNull ServiceVersionProvider versionProvider,
    @NonNull DockerClient dockerClient,
    @NonNull ServiceTaskProvider serviceTaskProvider,
    @NonNull DockerConfiguration configuration
  ) {
    super(nodeConfig, versionProvider);
    this.i18n = i18n;
    this.mainThread = tickLoop;
    this.eventManager = eventManager;
    this.cloudServiceManager = cloudServiceManager;
    this.dockerClient = dockerClient;
    this.serviceTaskProvider = serviceTaskProvider;
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
      this.i18n,
      this.mainThread,
      this.configuration,
      config,
      (InternalCloudServiceManager) manager,
      this.eventManager,
      this.versionProvider,
      preparer,
      this.serviceTaskProvider,
      this.dockerClient,
      this.dockerConfiguration);
  }

  @Override
  public @NonNull String name() {
    return this.dockerConfiguration.factoryName();
  }

  @Override
  protected boolean isPortInUse(@NonNull CloudServiceManager manager, @NonNull String hostAddress, int port) {
    // check if any local CloudNet service has the port
    if (this.isPortUsedByLocalService(manager, hostAddress, port)) {
      return true;
    }

    // check if any Docker container has this port binding
    if (this.isPortBoundInDocker(hostAddress, port)) {
      return true;
    }

    // validate that the port is free at OS level
    return this.isPortInUseAtOsLevel(hostAddress, port);
  }

  @Override
  protected boolean isPortInUseAtOsLevel(@NonNull String hostAddress, int port) {
    // only do OS-level port check if we can actually reach this address
    // this handles the case where CloudNet runs in a container with an external address configured
    if (!NetworkUtil.isBindableAddress(hostAddress)) {
      return false;
    }

    return super.isPortInUseAtOsLevel(hostAddress, port);
  }

  /**
   * Checks if any running Docker container has a port binding that would conflict
   * with the desired host address and port.
   */
  protected boolean isPortBoundInDocker(@NonNull String hostAddress, int port) {
    var containers = this.dockerClient.listContainersCmd()
      .withShowAll(false) // only running containers hold port bindings
      .exec();

    for (var container : containers) {
      var ports = container.getPorts();
      if (ports == null) {
        continue;
      }

      for (var binding : ports) {
        var publicPort = binding.getPublicPort();
        if (publicPort == null || publicPort != port) {
          continue;
        }

        var bindIp = binding.getIp();
        if (bindIp == null) {
          // exposed but not published to host - no conflict
          continue;
        }

        // conflict if:
        // 1. exact address match
        // 2. container binds all interfaces (0.0.0.0)
        // 3. we want all interfaces and container has any binding
        if (bindIp.equals(hostAddress)
            || "0.0.0.0".equals(bindIp)
            || "0.0.0.0".equals(hostAddress)) {
          return true;
        }
      }
    }

    return false;
  }
}
