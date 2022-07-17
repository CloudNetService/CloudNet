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

import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.service.CloudServiceFactory;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.util.NetworkUtil;
import java.util.Objects;
import lombok.NonNull;

public abstract class AbstractServiceFactory implements CloudServiceFactory {

  protected @NonNull ServiceConfiguration validateConfiguration(
    @NonNull CloudServiceManager manager,
    @NonNull ServiceConfiguration configuration
  ) {
    var configurationBuilder = ServiceConfiguration.builder(configuration);
    // set the node unique id
    configurationBuilder.node(Node.instance().nodeUniqueId());

    // set the environment type
    if (configuration.serviceId().environment() == null) {
      var env = Objects.requireNonNull(
        Node.instance().serviceVersionProvider().getEnvironmentType(configuration.serviceId().environmentName()),
        "Unknown environment type " + configuration.serviceId().environmentName());
      // set the environment type
      configurationBuilder.environment(env);
    }

    // find a free port for the service
    var port = configuration.port();
    while (this.isPortInUse(manager, port)) {
      port++;
    }
    // set the port
    return configurationBuilder.startPort(port).hostAddress(this.resolveHostAddress(configuration)).build();
  }

  protected @NonNull String resolveHostAddress(@NonNull ServiceConfiguration serviceConfiguration) {
    var hostAddress = serviceConfiguration.hostAddress();
    var fallbackHostAddress = Node.instance().config().hostAddress();
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
    var alias = Node.instance().config().ipAliases().get(hostAddress);
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

  protected boolean isPortInUse(@NonNull CloudServiceManager manager, int port) {
    // check if any local service has the port
    for (var cloudService : manager.localCloudServices()) {
      if (cloudService.serviceConfiguration().port() == port) {
        return true;
      }
    }
    // validate that the port is free
    return NetworkUtil.isInUse(port);
  }
}
