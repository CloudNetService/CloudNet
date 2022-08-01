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

package eu.cloudnetservice.node.service.defaults;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.event.service.CloudServiceNodeSelectEvent;
import eu.cloudnetservice.node.network.listener.message.ServiceChannelMessageListener;
import eu.cloudnetservice.node.service.CloudServiceManager;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NodeCloudServiceFactory implements CloudServiceFactory {

  private final EventManager eventManager;
  private final CloudServiceManager serviceManager;
  private final NodeServerProvider nodeServerProvider;

  private final Lock serviceCreationLock = new ReentrantLock(true);

  public NodeCloudServiceFactory(@NonNull Node nodeInstance) {
    this.eventManager = nodeInstance.eventManager();
    this.serviceManager = nodeInstance.cloudServiceProvider();
    this.nodeServerProvider = nodeInstance.nodeServerProvider();

    this.eventManager.registerListener(new ServiceChannelMessageListener(
      this.eventManager,
      this.serviceManager,
      this));
    nodeInstance.rpcFactory().newHandler(CloudServiceFactory.class, this).registerToDefaultRegistry();
  }

  @Override
  public @Nullable ServiceInfoSnapshot createCloudService(@NonNull ServiceConfiguration serviceConfiguration) {
    // check if this node can start services
    if (this.nodeServerProvider.localNode().head()) {
      // ensure that we're only creating one service
      this.serviceCreationLock.lock();
      try {
        // copy the configuration into a builder to prevent setting values on multiple objects which are then shared
        // over services which will eventually break the system
        var configurationBuilder = ServiceConfiguration.builder(serviceConfiguration);
        // prepare the service configuration
        this.replaceServiceId(serviceConfiguration, configurationBuilder);
        this.replaceServiceUniqueId(serviceConfiguration, configurationBuilder);
        this.includeGroupComponents(serviceConfiguration, configurationBuilder);

        var nodeSelectEvent = this.eventManager.callEvent(new CloudServiceNodeSelectEvent(serviceConfiguration));
        // check if we are allowed to start the service - return null otherwise
        if (nodeSelectEvent.cancelled()) {
          return null;
        }

        var nodeServer = nodeSelectEvent.nodeServer();
        if (nodeServer == null) {
          // no node was set by the event, try to select a node or return if no node can pick up the service
          nodeServer = this.serviceManager.selectNodeForService(serviceConfiguration);
          if (nodeServer == null) {
            return null;
          }
        }

        // use the selected node server and try to start the service
        if (nodeServer.channel() != null) {
          // send a request to start on the selected cluster node
          var service = this.sendNodeServerStartRequest(
            "head_node_to_node_start_service",
            nodeServer.info().uniqueId(),
            configurationBuilder.build());
          if (service != null) {
            // register the service locally in case the registration packet was not sent before a response to this
            // packet was received
            this.serviceManager.handleServiceUpdate(service, nodeServer.channel());
          }
          // return the service even if not given
          return service;
        } else {
          // start on the current node
          return this.serviceManager.createLocalCloudService(configurationBuilder.build()).serviceInfo();
        }
      } finally {
        this.serviceCreationLock.unlock();
      }
    } else {
      // send a request to the head node to start a service on the best node server
      return this.sendNodeServerStartRequest(
        "node_to_head_start_service",
        this.nodeServerProvider.headNode().info().uniqueId(),
        serviceConfiguration);
    }
  }


  protected @Nullable ServiceInfoSnapshot sendNodeServerStartRequest(
    @NonNull String message,
    @NonNull String targetNode,
    @NonNull ServiceConfiguration configuration
  ) {
    // send a request to the node to start a service
    var result = ChannelMessage.builder()
      .target(ChannelMessageTarget.Type.NODE, targetNode)
      .message(message)
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(configuration))
      .build()
      .sendSingleQueryAsync()
      .get(5, TimeUnit.SECONDS, null);
    // read the result service info from the buffer
    return result == null ? null : result.content().readObject(ServiceInfoSnapshot.class);
  }

  protected void includeGroupComponents(
    @NonNull ServiceConfiguration input,
    @NonNull ServiceConfiguration.Builder output
  ) {
    // include all groups which are matching the service configuration
    var groups = Node.instance().groupConfigurationProvider().groupConfigurations().stream()
      .filter(group -> group.targetEnvironments().contains(input.serviceId().environmentName()))
      .map(GroupConfiguration::name)
      .collect(Collectors.collectingAndThen(Collectors.toSet(), set -> {
        set.addAll(input.groups());
        return set;
      }));
    // update the groups which will get used
    output.groups(groups);
    // include each group component in the service configuration
    for (var group : groups) {
      // get the group
      var config = Node.instance().groupConfigurationProvider().groupConfiguration(group);
      // check if the config is available - add all components if so
      if (config != null) {
        output
          // components
          .modifyInclusions(inclusions -> inclusions.addAll(config.inclusions()))
          .modifyTemplates(templates -> templates.addAll(config.templates()))
          .modifyDeployments(deployments -> deployments.addAll(config.deployments()))
          // append process configuration settings
          .modifyJvmOptions(jvmOptions -> jvmOptions.addAll(config.jvmOptions()))
          .modifyProcessParameters(processParameters -> processParameters.addAll(config.processParameters()));
      }
    }
  }

  protected void replaceServiceId(@NonNull ServiceConfiguration input, @NonNull ServiceConfiguration.Builder output) {
    // check if the service id
    var serviceId = input.serviceId().taskServiceId();
    // check if the service id is invalid
    if (serviceId <= 0) {
      serviceId = 1;
    }
    // check if it is already taken
    var takenIds = this.serviceManager.servicesByTask(input.serviceId().taskName())
      .stream()
      .map(service -> service.serviceId().taskServiceId())
      .collect(Collectors.toSet());
    while (takenIds.contains(serviceId)) {
      serviceId++;
    }
    // update the service id
    output.taskId(serviceId);
  }

  protected void replaceServiceUniqueId(
    @NonNull ServiceConfiguration input,
    @NonNull ServiceConfiguration.Builder output
  ) {
    var uniqueId = input.serviceId().uniqueId();
    // check if the unique id is already taken
    while (this.serviceManager.service(uniqueId) != null) {
      uniqueId = UUID.randomUUID();
    }
    // set the new unique id
    output.uniqueId(uniqueId);
  }
}
