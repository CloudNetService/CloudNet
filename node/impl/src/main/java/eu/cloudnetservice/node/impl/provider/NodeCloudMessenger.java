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
import eu.cloudnetservice.node.impl.service.defaults.provider.EmptySpecificCloudServiceProvider;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Messenger implementation that relays channel messages to various targets within the cluster.
 *
 * @since 4.0
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
    var done = new AtomicBoolean();
    return this.sendChannelMessageQueryAsync(channelMessage, true)
      .thenApply(responses -> {
        // it might be that the timeout defined in the next step was already hit, therefore
        // this method already returned to the caller before a response is received. in this
        // case, we release the response we got immediately as it would leak otherwise
        var didComplete = done.compareAndSet(false, true);
        if (didComplete) {
          return responses;
        } else {
          responses.forEach(ChannelMessage::close);
          throw new IllegalStateException("received responses after downstream already completed");
        }
      })
      // hack: get a new incomplete future here so that the previous thenApply step runs as well.
      // the following orTimeout completes the future it's called on, so the releasing step would never run
      .thenApply(Function.identity())
      .orTimeout(DEFAULT_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      .whenComplete((_, _) -> done.set(true))
      .join();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    var done = new AtomicBoolean();
    return this.sendSingleChannelMessageQueryAsync(channelMessage)
      .thenApply(response -> {
        // it might be that the timeout defined in the next step was already hit, therefore
        // this method already returned to the caller before a response is received. in this
        // case, we release the response we got immediately as it would leak otherwise
        var didComplete = done.compareAndSet(false, true);
        if (didComplete) {
          return response;
        } else {
          response.close();
          throw new IllegalStateException("received response after downstream already completed");
        }
      })
      // hack: get a new incomplete future here so that the previous thenApply step runs as well.
      // the following orTimeout completes the future it's called on, so the releasing step would never run
      .thenApply(Function.identity())
      .orTimeout(DEFAULT_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      .whenComplete((_, _) -> done.set(true))
      .join();
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
    return this.sendChannelMessageQueryAsync(channelMessage).thenApply(responses -> {
      var responseCount = responses.size();
      return switch (responseCount) {
        case 0 -> null;
        case 1 -> Iterables.getOnlyElement(responses);
        default -> {
          // there were more than one response, so we need to close the
          // other responses to prevent leaking their content
          var responsesArray = responses.toArray(ChannelMessage[]::new);
          for (var index = 1; index < responsesArray.length; index++) {
            var response = responsesArray[index];
            response.close();
          }

          yield responsesArray[0];
        }
      };
    });
  }

  /**
   * Sends the given channel message to all provided targets, without waiting for any response.
   *
   * @param message              the channel message to send.
   * @param allowClusterRedirect if other nodes in the cluster are to be considered as targets.
   * @throws NullPointerException if the given channel message is null.
   */
  public void sendChannelMessage(@NonNull ChannelMessage message, boolean allowClusterRedirect) {
    try (var messageContent = message.content()) {
      var channels = this.findTargetChannels(message.targets(), allowClusterRedirect);
      if (channels.isEmpty()) {
        return;
      }

      for (var channel : channels) {
        messageContent.acquire(); // acquire once as the construct of ChannelMessagePacket releases the content
        var packet = new ChannelMessagePacket(message, false);
        if (message.sendSync()) {
          channel.sendPacketSync(packet);
        } else {
          channel.sendPacket(packet);
        }
      }
    }
  }

  /**
   * Sends the given channel message, waiting for a result of each sent message. If one receiver does not respond, the
   * returned future only completes after the configured query timeout of the target channel. Therefore, a timout is
   * advisable.
   *
   * @param message              the channel message to send.
   * @param allowClusterRedirect if other nodes in the cluster are to be considered as targets.
   * @throws NullPointerException if the given channel message is null.
   */
  public @NonNull CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NonNull ChannelMessage message,
    boolean allowClusterRedirect
  ) {
    try (var messageContent = message.content()) {
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
              if (responses != null) {
                results.addAll(responses);
              }
            } finally {
              resultContent.forceRelease();
            }
          })
          .whenComplete((_, _) -> resultTask.countDown())
          .exceptionally(thrown -> {
            LOGGER.debug("Exception while sending/receiving channel message query", thrown);
            return null;
          });
      }

      return resultTask;
    }
  }

  /**
   * Finds the corresponding channels for the given channel message targets.
   *
   * @param targets              the targets to find the channels for.
   * @param allowClusterRedirect if other nodes in the cluster are to be considered as targets.
   * @return the resolvable network channels for the given channel message targets.
   * @throws NullPointerException if the given targets collection is null.
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
   * Finds the corresponding network channels for the given channel message target.
   *
   * @param target               the channel message target to find the channels for.
   * @param allowClusterRedirect if other nodes in the cluster are to be considered as targets.
   * @return the resolvable network channels for the given channel message target.
   * @throws NullPointerException if the given channel message target is null.
   */
  protected @NonNull Collection<NetworkChannel> findTargetChannels(
    @NonNull ChannelMessageTarget target,
    boolean allowClusterRedirect
  ) {
    // special handling for all service targets, as they all need special handling. the first step extracts
    // the target service snapshots, the second step finds the network channel to which the message should
    // be sent (either the service directly for local services or the owning cluster node)
    var messageTargeType = target.type();
    Collection<ServiceInfoSnapshot> targetServiceSnapshots = switch (messageTargeType) {
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
        var environmentName = Objects.requireNonNull(target.name(), "ENVIRONMENT target without name");
        yield this.cloudServiceManager.servicesByEnvironment(environmentName);
      }
      case SERVICES_WITH_PROPERTY -> {
        var propertyKey = Objects.requireNonNull(target.name(), "PROPERTY target without name");
        yield this.cloudServiceManager.services().stream()
          .filter(service -> service.propertyHolder().contains(propertyKey))
          .toList();
      }
      default -> null; // not a service target
    };
    if (targetServiceSnapshots != null) {
      return targetServiceSnapshots.stream()
        .map(service -> this.cloudServiceManager.serviceProvider(service.serviceId().uniqueId()))
        .map(provider -> provider == EmptySpecificCloudServiceProvider.INSTANCE ? null : provider)
        .filter(Objects::nonNull)
        .map(provider -> {
          if (provider instanceof CloudService cloudService) {
            // service running locally on the current node
            return cloudService.networkChannel();
          } else if (allowClusterRedirect) {
            // service running on a remote node, redirect the message to the network channel of that node
            var serviceSnapshot = provider.serviceInfo();
            if (serviceSnapshot != null) {
              var nodeId = serviceSnapshot.serviceId().nodeUniqueId();
              var associatedNode = this.nodeServerProvider.node(nodeId);
              return associatedNode == null ? null : associatedNode.channel();
            }
          }

          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    }

    // targets one or more node in the cluster, only resolve these if allowed
    // to redirect the message within the cluster
    if (messageTargeType == ChannelMessageTarget.Type.NODE && allowClusterRedirect) {
      var nodeId = target.name();
      if (nodeId == null) {
        return this.nodeServerProvider.connectedNodeChannels();
      } else {
        var nodeServer = this.nodeServerProvider.node(nodeId);
        var channel = nodeServer == null ? null : nodeServer.channel();
        return channel == null ? List.of() : List.of(channel);
      }
    }

    // targets all components in the network, redirect to locally running services and all nodes
    if (messageTargeType == ChannelMessageTarget.Type.ALL) {
      var targetChannels = this.cloudServiceManager.localCloudServices().stream()
        .map(CloudService::networkChannel)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(HashSet::new));
      if (allowClusterRedirect) {
        targetChannels.addAll(this.nodeServerProvider.connectedNodeChannels());
      }

      return targetChannels;
    }

    return List.of(); // fall-through, not an error case
  }
}
