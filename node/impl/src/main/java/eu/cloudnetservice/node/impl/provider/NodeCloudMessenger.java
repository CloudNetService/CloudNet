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

package eu.cloudnetservice.node.impl.provider;

import com.google.common.collect.Iterables;
import dev.derklaro.aerogel.auto.annotation.Provides;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.impl.network.standard.ChannelMessagePacket;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.utils.base.concurrent.CountingTask;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of
 */
@Singleton
@Provides(CloudMessenger.class)
public class NodeCloudMessenger implements CloudMessenger {

  protected static final Type CHANNEL_MESSAGE_LIST_TYPE =
    TypeFactory.parameterizedClass(List.class, ChannelMessage.class);
  protected static final long DEFAULT_QUERY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20);

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeCloudMessenger.class);

  protected final NodeServerProvider nodeServerProvider;
  protected final CloudServiceManager cloudServiceManager;

  @Inject
  public NodeCloudMessenger(
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull CloudServiceManager cloudServiceManager
  ) {
    this.nodeServerProvider = nodeServerProvider;
    this.cloudServiceManager = cloudServiceManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendChannelMessage(@NonNull ChannelMessage channelMessage) {
    this.sendChannelMessage(channelMessage, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return this.sendChannelMessageQueryAsync(channelMessage, true)
      .orTimeout(DEFAULT_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      .join();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    var responses = this.sendChannelMessageQuery(channelMessage);
    return Iterables.getFirst(responses, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Void> sendChannelMessageAsync(@NonNull ChannelMessage channelMessage) {
    return TaskUtil.supplyAsync(() -> {
      this.sendChannelMessage(channelMessage);
      return null;
    });
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NonNull ChannelMessage message) {
    return this.sendChannelMessageQueryAsync(message, true);
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public CompletableFuture<ChannelMessage> sendSingleChannelMessageQueryAsync(@NonNull ChannelMessage channelMessage) {
    return TaskUtil.supplyAsync(() -> this.sendSingleChannelMessageQuery(channelMessage));
  }

  /**
   * @param message
   * @param allowClusterRedirect
   */
  public void sendChannelMessage(@NonNull ChannelMessage message, boolean allowClusterRedirect) {
    var messageContent = message.content();
    try {
      var channels = this.findTargetChannels(message.targets(), allowClusterRedirect);
      if (channels.isEmpty()) {
        return;
      }

      for (var channel : channels) {
        messageContent.acquire(); // acquire once as the construct of ChannelMessagePacket releases the content
        var packet = new ChannelMessagePacket(message, false);
        channel.sendPacketSync(packet);
      }
    } finally {
      messageContent.release();
    }
  }

  /**
   * @param message
   * @param allowClusterRedirect
   * @return
   */
  public @NonNull CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NonNull ChannelMessage message,
    boolean allowClusterRedirect
  ) {
    var messageContent = message.content();
    try {
      var channels = this.findTargetChannels(message.targets(), allowClusterRedirect);
      if (channels.isEmpty()) {
        return CompletableFuture.completedFuture(new ArrayList<>());
      }

      var results = new ArrayList<ChannelMessage>();
      var resultTask = new CountingTask<Collection<ChannelMessage>>(results, channels.size());
      for (var channel : channels) {
        messageContent.acquire(); // acquire once as the construct of ChannelMessagePacket releases the content
        var packet = new ChannelMessagePacket(message, false);
        channel.sendQueryAsync(packet)
          .thenAccept(result -> {
            var resultContent = result.content();
            try {
              Collection<ChannelMessage> responses = resultContent.readObject(CHANNEL_MESSAGE_LIST_TYPE);
              results.addAll(responses);
            } finally {
              resultContent.release();
            }
          })
          .whenComplete((_, _) -> resultTask.countDown())
          .exceptionally(thrown -> {
            LOGGER.debug("Exception while sending/receiving channel message query", thrown);
            return null;
          });
      }

      return resultTask;
    } finally {
      messageContent.release();
    }
  }

  /**
   * @param targets
   * @param allowClusterRedirect
   * @return
   */
  protected @NonNull Collection<NetworkChannel> findTargetChannels(
    @NonNull Collection<ChannelMessageTarget> targets,
    boolean allowClusterRedirect
  ) {
    var targetCount = targets.size();
    return switch (targetCount) {
      case 0 -> Set.of();
      case 1 -> {
        var target = Iterables.getOnlyElement(targets);
        yield this.findTargetChannels(target, allowClusterRedirect);
      }
      default -> targets.stream()
        .flatMap(target -> this.findTargetChannels(target, allowClusterRedirect).stream())
        .collect(Collectors.toUnmodifiableSet());
    };
  }

  /**
   *
   * @param target
   * @param allowClusterRedirect
   * @return
   */
  protected @NonNull Collection<NetworkChannel> findTargetChannels(
    @NonNull ChannelMessageTarget target,
    boolean allowClusterRedirect
  ) {
    // special handling for all service targets, as they all need special handling. the first step extracts
    // the target service snapshots, the second step finds the network channel to which the message should
    // be sent (either the service directly for local services or the owning cluster node)
    Collection<ServiceInfoSnapshot> targetServiceSnapshots = switch (target.type()) {
      case SERVICE -> {
        var serviceName = target.name();
        if (serviceName != null) {
          var serviceInfo = this.cloudServiceManager.serviceByName(serviceName);
          yield serviceInfo == null ? List.of() : List.of(serviceInfo);
        } else {
          yield this.cloudServiceManager.runningServices();
        }
      }
      case SERVICES_BY_TASK -> {
        var taskName = Objects.requireNonNull(target.name(), "TASK target without name");
        yield this.cloudServiceManager.servicesByTask(taskName);
      }
      case SERVICES_BY_GROUP -> {
        var groupName = Objects.requireNonNull(target.name(), "GROUP target without name");
        yield this.cloudServiceManager.servicesByGroup(groupName);
      }
      case SERVICES_BY_ENV -> {
        var environment = Objects.requireNonNull(target.environment(), "ENVIRONMENT target without name");
        yield this.cloudServiceManager.servicesByEnvironment(environment.name());
      }
    }



    switch (target.type()) {
      // just include all known channels
      case ALL -> {
        Set<NetworkChannel> result = new HashSet<>();
        // all local services
        this.cloudServiceManager.localCloudServices().stream()
          .map(CloudService::networkChannel)
          .filter(Objects::nonNull)
          .forEach(result::add);
        // all connected nodes
        if (allowClusterRedirect) {
          result.addAll(this.nodeServerProvider.connectedNodeChannels());
        }
        return result;
      }
      case NODES -> {
        // search for the matching node server
        if (allowClusterRedirect) {
          // check if a specific node server was selected or all node servers are targeted
          if (target.name() == null) {
            return this.nodeServerProvider.connectedNodeChannels();
          }
          // check if we know the target node server
          var server = this.nodeServerProvider.node(target.name());
          return server == null || server.channel() == null
            ? Collections.emptySet()
            : Collections.singleton(server.channel());
        } else {
          // not allowed to redirect the message
          return Collections.emptySet();
        }
      }
      case SERVICE -> {
        // check if a specific service was requested
        if (target.name() == null) {
          // if no specific name is given just get all local channels
          var channels = this.cloudServiceManager.localCloudServices().stream()
            .map(CloudService::networkChannel)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
          // check if cluster redirect is allowed - add all connected node channels then
          if (allowClusterRedirect) {
            channels.addAll(this.nodeServerProvider.connectedNodeChannels());
          }
          // return here
          return channels;
        } else {
          // check if the service is running locally - use the known channel then
          var localService = this.cloudServiceManager.localCloudService(target.name());
          if (localService != null) {
            return localService.networkChannel() == null
              ? Collections.emptySet()
              : Collections.singleton(localService.networkChannel());
          }
        }
        // check if we are allowed to redirect the message to the node running the service
        if (allowClusterRedirect) {
          // if no specific service is given just send it to all nodes
          if (target.name() == null) {
            return this.nodeServerProvider.connectedNodeChannels();
          }
          // check if we know the service from the cluster
          var service = this.cloudServiceManager.serviceByName(target.name());
          if (service != null) {
            // check if we know the target node server to send the channel message to instead
            var server = this.nodeServerProvider.node(service.serviceId().nodeUniqueId());
            return server == null || server.channel() == null
              ? Collections.emptySet()
              : Collections.singleton(server.channel());
          }
        }
        // unable to retrieve information about the target - just an empty set then
        return Collections.emptySet();
      }
      case SERVICES_BY_TASK -> {
        // lookup all services of the given task
        return this.filterChannels(
          this.cloudServiceManager.servicesByTask(target.name()),
          allowClusterRedirect);
      }
      case SERVICES_BY_ENV -> {
        // lookup all services of the given environment
        return this.filterChannels(
          this.cloudServiceManager.servicesByEnvironment(target.environment().name()),
          allowClusterRedirect);
      }
      case SERVICES_BY_GROUP -> {
        // lookup all services of the given group
        return this.filterChannels(
          this.cloudServiceManager.servicesByGroup(target.name()),
          allowClusterRedirect);
      }
      default -> throw new IllegalArgumentException("Unhandled ChannelMessageTarget.Type: " + target.type());
    }
  }

  protected @NonNull Collection<NetworkChannel> filterChannels(
    @NonNull Collection<ServiceInfoSnapshot> snapshots,
    boolean allowClusterRedirect
  ) {
    return snapshots.stream()
      .map(service -> {
        // check if the service is running locally
        var localService = this.cloudServiceManager.localCloudService(service.serviceId().name());
        if (localService != null) {
          return localService.networkChannel();
        }
        // check if we are allowed to redirect the message to the node running the service
        if (allowClusterRedirect) {
          // check if we know the node on which the service is running
          var nodeServer = this.nodeServerProvider.node(service.serviceId().nodeUniqueId());
          return nodeServer == null ? null : nodeServer.channel();
        }
        // no target found
        return null;
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }
}
