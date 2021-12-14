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

package de.dytanic.cloudnet.service.defaults;

import com.google.common.collect.ComparisonChain;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget.Type;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.network.listener.message.ServiceChannelMessageListener;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeCloudServiceFactory implements CloudServiceFactory {

  private final ICloudServiceManager serviceManager;
  private final IClusterNodeServerProvider nodeServerProvider;

  public NodeCloudServiceFactory(@NotNull CloudNet nodeInstance) {
    this.serviceManager = nodeInstance.getCloudServiceProvider();
    this.nodeServerProvider = nodeInstance.getClusterNodeServerProvider();

    nodeInstance.getEventManager().registerListener(new ServiceChannelMessageListener(
      nodeInstance.getEventManager(),
      this.serviceManager,
      this));
    nodeInstance.getRPCProviderFactory().newHandler(CloudServiceFactory.class, this).registerToDefaultRegistry();
  }

  @Override
  public @Nullable ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
    // check if this node can start services
    if (this.nodeServerProvider.getHeadNode().equals(this.nodeServerProvider.getSelfNode())) {
      // prepare the service configuration
      this.replaceServiceId(serviceConfiguration);
      this.replaceServiceUniqueId(serviceConfiguration);
      this.includeGroupComponents(serviceConfiguration);
      // get the logic node server to start the service on
      var nodeServer = this.peekLogicNodeServer(serviceConfiguration);
      // if there is a node server send a request to start a service
      if (nodeServer != null) {
        return this.sendNodeServerStartRequest(
          "head_node_to_node_start_service",
          nodeServer.getNodeInfo().getUniqueId(),
          serviceConfiguration);
      }
      // start the service on the local node
      return this.serviceManager.createLocalCloudService(serviceConfiguration).getServiceInfoSnapshot();
    } else {
      // send a request to the head node to start a service on the best node server
      return this.sendNodeServerStartRequest(
        "node_to_head_start_service",
        this.nodeServerProvider.getHeadNode().getNodeInfo().getUniqueId(),
        serviceConfiguration);
    }
  }

  protected @Nullable ServiceInfoSnapshot sendNodeServerStartRequest(
    @NotNull String message,
    @NotNull String targetNode,
    @NotNull ServiceConfiguration configuration
  ) {
    // send a request to the node to start a service
    var result = ChannelMessage.builder()
      .target(Type.NODE, targetNode)
      .message(message)
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBufFactory.defaultFactory().createEmpty().writeObject(configuration))
      .build()
      .sendSingleQueryAsync()
      .get(5, TimeUnit.SECONDS, null);
    // read the result service info from the buffer
    return result == null ? null : result.content().readObject(ServiceInfoSnapshot.class);
  }

  protected @Nullable IClusterNodeServer peekLogicNodeServer(@NotNull ServiceConfiguration configuration) {
    // check if the node is already specified
    if (configuration.getServiceId().getNodeUniqueId() != null) {
      var server = this.nodeServerProvider.getNodeServer(configuration.getServiceId().getNodeUniqueId());
      return server == null || !server.isAvailable() ? null : server;
    }
    // extract the max heap memory from the snapshot which will be used for later memory usage comparison
    var mh = configuration.getProcessConfig().getMaxHeapMemorySize();
    // find the best node server
    return this.nodeServerProvider.getNodeServers().stream()
      .filter(IClusterNodeServer::isAvailable)
      // only allow service start on nodes that are not marked for draining
      .filter(nodeServer -> !nodeServer.getNodeInfoSnapshot().isDrain())
      .filter(server -> {
        var allowedNodes = configuration.getServiceId().getAllowedNodes();
        return allowedNodes.isEmpty() || allowedNodes.contains(server.getNodeInfo().getUniqueId());
      })
      .min((left, right) -> {
        // begin by comparing the heap memory usage
        var chain = ComparisonChain.start()
          .compare(left.getNodeInfoSnapshot().getUsedMemory() + mh, right.getNodeInfoSnapshot().getUsedMemory() + mh);
        // only include the cpu usage if both nodes can provide a value
        if (left.getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage() >= 0
          && right.getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage() >= 0) {
          // add the system usage to the chain
          chain = chain.compare(
            left.getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage(),
            right.getNodeInfoSnapshot().getProcessSnapshot().getSystemCpuUsage());
        }
        // use the result of the comparison
        return chain.result();
      }).orElse(null);
  }

  protected void includeGroupComponents(@NotNull ServiceConfiguration configuration) {
    // include all groups which are matching the service configuration
    CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
      .filter(group -> group.getTargetEnvironments().contains(configuration.getServiceId().getEnvironmentName()))
      .forEach(group -> configuration.getGroups().add(group.name()));
    // include each group component in the service configuration
    for (var group : configuration.getGroups()) {
      // get the group
      var config = CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfiguration(group);
      // check if the config is available - add all components if so
      if (config != null) {
        configuration.getIncludes().addAll(config.getIncludes());
        configuration.getTemplates().addAll(config.getTemplates());
        configuration.getDeployments().addAll(config.getDeployments());

        configuration.getProcessConfig().getJvmOptions().addAll(config.getJvmOptions());
        configuration.getProcessConfig().getProcessParameters().addAll(config.getProcessParameters());
      }
    }
  }

  protected void replaceServiceId(@NotNull ServiceConfiguration config) {
    // check if the service id
    var serviceId = config.getServiceId().getTaskServiceId();
    // check if the service id is invalid
    if (serviceId <= 0) {
      serviceId = 1;
    }
    // check if it is already taken
    Collection<Integer> takenIds = this.serviceManager.getCloudServicesByTask(config.getServiceId().getTaskName())
      .stream()
      .map(service -> service.getServiceId().getTaskServiceId())
      .collect(Collectors.toSet());
    while (takenIds.contains(serviceId)) {
      serviceId++;
    }
    // update the service id
    config.getServiceId().setTaskServiceId(serviceId);
  }

  protected void replaceServiceUniqueId(@NotNull ServiceConfiguration config) {
    var uniqueId = config.getServiceId().getUniqueId();
    // check if the unique id is already taken
    while (this.serviceManager.getCloudService(uniqueId) != null) {
      uniqueId = UUID.randomUUID();
    }
    // set the new unique id
    config.getServiceId().setUniqueId(uniqueId);
  }
}
