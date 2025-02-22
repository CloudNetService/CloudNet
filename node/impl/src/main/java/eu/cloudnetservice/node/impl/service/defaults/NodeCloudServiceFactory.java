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

package eu.cloudnetservice.node.impl.service.defaults;

import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceCreateRetryConfiguration;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.event.service.CloudServiceConfigurationPrePrepareEvent;
import eu.cloudnetservice.node.event.service.CloudServiceNodeSelectEvent;
import eu.cloudnetservice.node.impl.network.listener.message.ServiceChannelMessageListener;
import eu.cloudnetservice.node.impl.service.InternalCloudServiceManager;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Provides(CloudServiceFactory.class)
public class NodeCloudServiceFactory implements CloudServiceFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeCloudServiceFactory.class);

  private final EventManager eventManager;
  private final InternalCloudServiceManager serviceManager;
  private final NodeServerProvider nodeServerProvider;
  private final GroupConfigurationProvider groupProvider;

  private final Lock serviceCreationLock = new ReentrantLock(true);
  private final ScheduledExecutorService createRetryExecutor = Executors.newSingleThreadScheduledExecutor();

  @Inject
  public NodeCloudServiceFactory(
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull RPCHandlerRegistry handlerRegistry,
    @NonNull InternalCloudServiceManager serviceManager,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull GroupConfigurationProvider groupProvider
  ) {
    this.eventManager = eventManager;
    this.serviceManager = serviceManager;
    this.nodeServerProvider = nodeServerProvider;
    this.groupProvider = groupProvider;

    var rpcHandler = rpcFactory.newRPCHandlerBuilder(CloudServiceFactory.class).targetInstance(this).build();
    handlerRegistry.registerHandler(rpcHandler);
  }

  @PostConstruct
  private void registerServiceChannelListener() {
    this.eventManager.registerListener(ServiceChannelMessageListener.class);
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
        this.eventManager.callEvent(new CloudServiceConfigurationPrePrepareEvent(
          this.serviceManager,
          maybeServiceConfiguration,
          configurationBuilder));

        // prepare the service configuration
        this.replaceServiceId(maybeServiceConfiguration, configurationBuilder);
        this.replaceServiceUniqueId(maybeServiceConfiguration, configurationBuilder);
        this.includeGroupComponents(maybeServiceConfiguration, configurationBuilder);

        // disable retries on the new configuration, we only schedule them based on the original one
        configurationBuilder.retryConfiguration(ServiceCreateRetryConfiguration.NO_RETRY);

        // finish the replaced configuration & get the logic node server to start the service on
        var serviceConfiguration = configurationBuilder.build();
        var nodeSelectEvent = this.eventManager.callEvent(new CloudServiceNodeSelectEvent(
          this.serviceManager,
          serviceConfiguration));
        // check if we are allowed to start the service - return null otherwise
        if (nodeSelectEvent.cancelled()) {
          return this.scheduleCreateRetryIfEnabled(
            maybeServiceConfiguration.retryConfiguration(),
            serviceConfiguration);
        }

        var nodeServer = nodeSelectEvent.nodeServer();
        if (nodeServer == null) {
          // no node was set by the event, try to select a node or return if no node can pick up the service
          nodeServer = this.serviceManager.selectNodeForService(serviceConfiguration);
          if (nodeServer == null) {
            return this.scheduleCreateRetryIfEnabled(
              maybeServiceConfiguration.retryConfiguration(),
              serviceConfiguration);
          }
        }

        // if there is a node server send a request to start a service
        if (nodeServer.channel() != null) {
          // send a request to start on the selected cluster node
          var createResult = this.sendNodeServerStartRequest(
            "head_node_to_node_start_service",
            nodeServer.info().uniqueId(),
            serviceConfiguration);

          // process the service creation result and return it if the creation was successful
          createResult = this.processServiceStartResponse(createResult, nodeServer);
          if (createResult.state() == ServiceCreateResult.State.CREATED) {
            return createResult;
          }

          // service creation failed - retry
          return this.scheduleCreateRetryIfEnabled(
            maybeServiceConfiguration.retryConfiguration(),
            serviceConfiguration);
        } else {
          // start on the current node & publish the service snapshot to all components
          var createdService = this.serviceManager.createLocalCloudService(serviceConfiguration);
          createdService.handleServiceRegister();

          // construct the create result
          return ServiceCreateResult.created(createdService.serviceInfo());
        }
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

  @Override
  public @NonNull CompletableFuture<ServiceCreateResult> createCloudServiceAsync(
    @NonNull ServiceConfiguration configuration
  ) {
    return CompletableFuture.supplyAsync(() -> this.createCloudService(configuration));
  }

  protected @NonNull ServiceCreateResult processServiceStartResponse(
    @NonNull ServiceCreateResult result,
    @NonNull NodeServer associatedNode
  ) {
    // if the service creation failed we don't need to check anything
    if (result.state() != ServiceCreateResult.State.CREATED) {
      return result;
    }

    // check if the node is still connected
    var nodeChannel = associatedNode.channel();
    if (nodeChannel == null || !associatedNode.available()) {
      LOGGER.debug(
        "Unable to register service on node {} as the node is no longer connected",
        associatedNode.info().uniqueId());
      return ServiceCreateResult.FAILED;
    }

    // try to register the created service locally
    var serviceUniqueId = result.serviceInfo().serviceId().uniqueId();
    var serviceProvider = this.serviceManager.registerService(result.serviceInfo(), nodeChannel);
    if (serviceProvider != null) {
      // service registered successfully, finish the registration of the service on the other node
      ChannelMessage.builder()
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .message("head_node_to_node_finish_service_registration")
        .buffer(DataBuf.empty().writeUniqueId(serviceUniqueId))
        .target(ChannelMessageTarget.Type.NODE, associatedNode.info().uniqueId())
        .build()
        .send();
      return result;
    } else {
      // a service with the id already exists, just let the unaccepted service
      // time out on the other node... ¯\_(ツ)_/¯
      return ServiceCreateResult.FAILED;
    }
  }

  protected @NonNull ServiceCreateResult sendNodeServerStartRequest(
    @NonNull String message,
    @NonNull String targetNode,
    @NonNull ServiceConfiguration configuration
  ) {
    // send a request to the node to start a service
    var future = ChannelMessage.builder()
      .target(ChannelMessageTarget.Type.NODE, targetNode)
      .message(message)
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(configuration))
      .build()
      .sendSingleQueryAsync();
    var result = TaskUtil.getOrDefault(future, Duration.ofSeconds(20), null);

    // read the result service info from the buffer, if the there was no response then we need to fail (only the head
    // node should queue start requests)
    var createResult = result == null ? null : result.content().readObject(ServiceCreateResult.class);
    return Objects.requireNonNullElse(createResult, ServiceCreateResult.FAILED);
  }

  protected @NonNull ServiceCreateResult scheduleCreateRetryIfEnabled(
    @NonNull ServiceCreateRetryConfiguration retryConfiguration,
    @NonNull ServiceConfiguration serviceConfiguration
  ) {
    // check if we need to retry the service creation
    if (!retryConfiguration.enabled()) {
      return ServiceCreateResult.FAILED;
    }

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
    var groups = this.groupProvider.groupConfigurations().stream()
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
      var config = this.groupProvider.groupConfiguration(group);
      // check if the config is available - add all components if so
      if (config != null) {
        output
          // components
          .modifyInclusions(inclusions -> inclusions.addAll(config.inclusions()))
          .modifyTemplates(templates -> templates.addAll(config.templates()))
          .modifyDeployments(deployments -> deployments.addAll(config.deployments()))
          // append process configuration settings
          .modifyJvmOptions(jvmOptions -> jvmOptions.addAll(config.jvmOptions()))
          .modifyEnvironmentVariables(environment -> environment.putAll(config.environmentVariables()))
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
