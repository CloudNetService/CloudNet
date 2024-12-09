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

package eu.cloudnetservice.node.impl.service.defaults.factory;

import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import eu.cloudnetservice.node.impl.version.ServiceVersionProvider;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.service.LocalCloudServiceFactory;
import java.util.Objects;
import lombok.NonNull;

public abstract class BaseLocalCloudServiceFactory implements LocalCloudServiceFactory {

  protected final Configuration configuration;
  protected final ServiceVersionProvider versionProvider;

  protected BaseLocalCloudServiceFactory(
    @NonNull Configuration configuration,
    @NonNull ServiceVersionProvider versionProvider
  ) {
    this.configuration = configuration;
    this.versionProvider = versionProvider;
  }

  protected @NonNull ServiceConfiguration validateConfiguration(
    @NonNull CloudServiceManager manager,
    @NonNull ServiceConfiguration configuration
  ) {
    var configurationBuilder = ServiceConfiguration.builder(configuration);

    // set the node unique id
    configurationBuilder.node(this.configuration.identity().uniqueId());

    // set the environment type
    if (configuration.serviceId().environment() == null) {
      var env = Objects.requireNonNull(
        this.versionProvider.environmentType(configuration.serviceId().environmentName()),
        "Unknown environment type " + configuration.serviceId().environmentName());
      // set the environment type
      configurationBuilder.environment(env);
    }

    // resolve the host address & port we should use for the service
    var hostAddress = this.resolveHostAddress(configuration);
    var hostPort = this.findFreeServicePort(manager, configuration, hostAddress);

    // apply the address to the configuration & finish up
    return configurationBuilder.startPort(hostPort).hostAddress(hostAddress).build();
  }

  protected @NonNull String resolveHostAddress(@NonNull ServiceConfiguration serviceConfiguration) {
    var hostAddress = serviceConfiguration.hostAddress();
    var fallbackHostAddress = this.configuration.hostAddress();
    // if null is supplied use fallback address
    if (hostAddress == null) {
      return fallbackHostAddress;
    }

    // use the supplied host address if it is an inet address
    var hostAndPort = NetworkUtil.parseAssignableHostAndPort(hostAddress, false);
    if (hostAndPort != null) {
      return hostAndPort.host();
    }

    // retrieve the alias from the node
    var alias = this.configuration.ipAliases().get(hostAddress);
    if (alias == null) {
      return fallbackHostAddress;
    }

    // check if the alias is a valid inet address
    var aliasHost = NetworkUtil.parseAssignableHostAndPort(alias, false);
    if (aliasHost != null) {
      return aliasHost.host();
    }

    // explode, resolved alias is not an address
    throw new IllegalArgumentException(String.format(
      "The host address %s of the alias %s is not a valid inet address.",
      alias,
      hostAddress));
  }

  protected int findFreeServicePort(
    @NonNull CloudServiceManager manager,
    @NonNull ServiceConfiguration configuration,
    @NonNull String hostAddress
  ) {
    // increase the port number until we found a port
    var port = configuration.port();
    while (this.isPortInUse(manager, hostAddress, port)) {
      port++;

      // stop if the port exceeds the possible port range
      if (port > 0xFFFF) {
        throw new IllegalStateException("No free port found for service, started at port: " + configuration.port());
      }
    }

    // use the next free, available port
    return port;
  }

  protected boolean isPortInUse(@NonNull CloudServiceManager manager, @NonNull String hostAddress, int port) {
    // check if any local service has the port
    for (var cloudService : manager.localCloudServices()) {
      var address = cloudService.serviceInfo().address();
      if (address.host().equals(hostAddress) && address.port() == port) {
        return true;
      }
    }

    // validate that the port is free
    return NetworkUtil.isInUse(hostAddress, port);
  }
}
