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

import com.google.common.collect.ComparisonChain;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceCreateRetryConfiguration;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.network.listener.message.ServiceChannelMessageListener;
import eu.cloudnetservice.node.service.CloudServiceManager;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NodeCloudServiceFactory implements CloudServiceFactory {

  private final CloudServiceManager serviceManager;
  private final NodeServerProvider nodeServerProvider;

  private final Lock serviceCreationLock = new ReentrantLock(true);
  private final ScheduledExecutorService createRetryExecutor = Executors.newSingleThreadScheduledExecutor();

  public NodeCloudServiceFactory(@NonNull Node nodeInstance) {
    this.serviceManager = nodeInstance.cloudServiceProvider();
    this.nodeServerProvider = nodeInstance.nodeServerProvider();

    nodeInstance.eventManager().registerListener(new ServiceChannelMessageListener(
      nodeInstance.eventManager(),
      this.serviceManager,
      this));
    nodeInstance.rpcFactory().newHandler(CloudServiceFactory.class, this).registerToDefaultRegistry();
  }

  @Override
  public @NonNull ServiceCreateResult createCloudService(@NonNull ServiceConfiguration maybeServiceConfiguration) {
    // check if this node can start services
    if (this.nodeServerProvider.localNode().head()) {
      // ensure that we're only creating one service
      this.serviceCreationLock.lock();
      try {
        // copy the configuration into a builder to prevent setting values on multiple objects which are then shared
        // over services which will eventually break the system
        var configurationBuilder = ServiceConfiguration.builder(maybeServiceConfiguration);
        // prepare the service configuration
        this.replaceServiceId(maybeServiceConfiguration, configurationBuilder);
        this.replaceServiceUniqueId(maybeServiceConfiguration, configurationBuilder);
        this.includeGroupComponents(maybeServiceConfiguration, configurationBuilder);
        // disable retries on the new configuration, we only schedule them based on the original one
        configurationBuilder.retryConfiguration(ServiceCreateRetryConfiguration.NO_RETRY);

        // finish the replaced configuration & get the logic node server to start the service on
        var serviceConfiguration = configurationBuilder.build();
        var nodeServer = this.peekLogicNodeServer(serviceConfiguration);
        // if there is a node server send a request to start a service
        if (nodeServer != null) {
          if (nodeServer.channel() != null) {
            // send a request to start on the selected cluster node
            var createResult = this.sendNodeServerStartRequest(
              "head_node_to_node_start_service",
              nodeServer.info().uniqueId(),
              configurationBuilder.build());
            if (createResult.state() == ServiceCreateResult.State.CREATED) {
              // register the service locally in case the registration packet was not sent before a response to this
              // packet was received
              this.serviceManager.handleServiceUpdate(createResult.serviceInfo(), nodeServer.channel());
            }

            return createResult;
          } else {
            // start on the current node
            var serviceInfo = this.serviceManager.createLocalCloudService(configurationBuilder.build()).serviceInfo();
            return ServiceCreateResult.created(serviceInfo);
          }
        }

        // unable to find a node to start the service on, check if we should requeue the service creation
        var retryConfiguration = maybeServiceConfiguration.retryConfiguration();
        if (retryConfiguration.enabled()) {
          return this.scheduleCreateRetry(retryConfiguration, serviceConfiguration);
        }

        // unable to start, and no retry was requested
        return ServiceCreateResult.FAILED;
      } finally {
        this.serviceCreationLock.unlock();
      }
    } else {
      // send a request to the head node to start a service on the best node server
      return this.sendNodeServerStartRequest(
        "node_to_head_start_service",
        this.nodeServerProvider.headNode().info().uniqueId(),
        maybeServiceConfiguration);
    }
  }

  protected @NonNull ServiceCreateResult sendNodeServerStartRequest(
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

    // read the result service info from the buffer, if the there was no response then we need to fail (only the head
    // node should queue start requests)
    var createResult = result == null ? null : result.content().readObject(ServiceCreateResult.class);
    return Objects.requireNonNullElse(createResult, ServiceCreateResult.FAILED);
  }

  protected @Nullable NodeServer peekLogicNodeServer(@NonNull ServiceConfiguration configuration) {
    // check if the node is already specified
    if (configuration.serviceId().nodeUniqueId() != null) {
      // check for a cluster node server
      var server = this.nodeServerProvider.node(configuration.serviceId().nodeUniqueId());
      if (server != null) {
        // the requested node is a cluster node, check if that node is still accepting services
        return !server.available() || server.nodeInfoSnapshot().draining() ? null : server;
      }
      // no node server with the given name which can start services found
      return null;
    }

    // find the best node server
    return this.nodeServerProvider.nodeServers().stream()
      .filter(NodeServer::available)
      .filter(nodeServer -> !nodeServer.nodeInfoSnapshot().draining())
      .filter(server -> {
        var allowedNodes = configuration.serviceId().allowedNodes();
        return allowedNodes.isEmpty() || allowedNodes.contains(server.info().uniqueId());
      })
      .min((left, right) -> {
        // calculate the reserved memory amount based on the cached service information on this node
        // this is the better way to do this, as newly created services on other nodes will get cached instantly, rather
        // than us needing to wait for the updated node info to be sent by the associated node. In normal scenarios
        // that is not a big problem, however when many start requests are coming in, that can lead to one node picking
        // up a lot of services until (only a few ms later) the updated snapshot is present.
        var leftReservedMemory = this.calculateReservedMemoryPercentage(left);
        var rightReservedMemory = this.calculateReservedMemoryPercentage(right);

        // we elevate the used heap memory percentage over the cpu usage, as it's varying much more
        var chain = ComparisonChain.start().compare(leftReservedMemory, rightReservedMemory);
        // only include the cpu usage if both nodes can provide a value
        if (left.nodeInfoSnapshot().processSnapshot().systemCpuUsage() >= 0
          && right.nodeInfoSnapshot().processSnapshot().systemCpuUsage() >= 0) {
          // add the system usage to the chain
          chain = chain.compare(
            left.nodeInfoSnapshot().processSnapshot().systemCpuUsage(),
            right.nodeInfoSnapshot().processSnapshot().systemCpuUsage());
        }
        // use the result of the comparison
        return chain.result();
      }).orElse(null);
  }

  protected @NonNull ServiceCreateResult scheduleCreateRetry(
    @NonNull ServiceCreateRetryConfiguration retryConfiguration,
    @NonNull ServiceConfiguration serviceConfiguration
  ) {
    var tracker = new ServiceCreateRetryTracker(
      serviceConfiguration,
      this.createRetryExecutor,
      this,
      retryConfiguration);

    // schedule the backoff task - we don't allow 0 as the first retry delay in order to allow plugins
    // to at least get an idea of what happened before the next start try
    var retryDelay = Math.max(500L, tracker.nextRetryDelay());
    this.createRetryExecutor.schedule(tracker, retryDelay, TimeUnit.MILLISECONDS);

    // create a new result which indicates that the service create was deferred, including the id to which the
    // state events will be sent if the service gets created successfully later
    return ServiceCreateResult.deferred(tracker.creationId());
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

  protected int calculateReservedMemoryPercentage(@NonNull NodeServer server) {
    // get the reserved memory on the given node based on the services which are running on it and sum it up
    var reservedMemory = this.serviceManager.services().stream()
      .filter(info -> info.serviceId().nodeUniqueId().equals(server.name()))
      .mapToInt(info -> info.configuration().processConfig().maxHeapMemorySize())
      .sum();
    // convert to a percentage
    return (reservedMemory * 100) / server.nodeInfoSnapshot().maxMemory();
  }
}
