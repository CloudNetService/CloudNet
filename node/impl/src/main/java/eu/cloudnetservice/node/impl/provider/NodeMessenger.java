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
import dev.derklaro.aerogel.auto.Provides;
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
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides(CloudMessenger.class)
public class NodeMessenger implements CloudMessenger {

  protected static final Type COL_MSG = TypeFactory.parameterizedClass(Collection.class, ChannelMessage.class);

  protected final NodeServerProvider nodeServerProvider;
  protected final CloudServiceManager cloudServiceManager;

  @Inject
  public NodeMessenger(
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull CloudServiceManager cloudServiceManager
  ) {
    this.nodeServerProvider = nodeServerProvider;
    this.cloudServiceManager = cloudServiceManager;
  }

  @Override
  public void sendChannelMessage(@NonNull ChannelMessage message) {
    this.sendChannelMessage(message, true);
  }

  @Override
  public @NonNull CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NonNull ChannelMessage message
  ) {
    return this.sendChannelMessageQueryAsync(message, true);
  }

  @Override
  public @NonNull CompletableFuture<ChannelMessage> sendSingleChannelMessageQueryAsync(
    @NonNull ChannelMessage channelMessage
  ) {
    return TaskUtil.supplyAsync(() -> this.sendSingleChannelMessageQuery(channelMessage));
  }

  @Override
  public @NonNull Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return TaskUtil.getOrDefault(this.sendChannelMessageQueryAsync(channelMessage), Duration.ofSeconds(20), List.of());
  }

  @Override
  public @Nullable ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return Iterables.getFirst(this.sendChannelMessageQuery(channelMessage), null);
  }

  @Override
  public @NonNull CompletableFuture<Void> sendChannelMessageAsync(@NonNull ChannelMessage channelMessage) {
    return TaskUtil.runAsync(() -> this.sendChannelMessage(channelMessage));
  }

  public void sendChannelMessage(@NonNull ChannelMessage message, boolean allowClusterRedirect) {
    // find the target channels to send the message to
    var channels = this.findChannels(message.targets(), allowClusterRedirect);
    for (var channel : channels) {
      // acquire the message content for each channel we're sending the message to
      // when writing the message content to the target buffer, the message is released
      // that means when the message was written to all channels it's released unless someone acquired it before
      message.content().acquire();

      // construct and send the packet
      var packet = new ChannelMessagePacket(message, false);
      if (message.sendSync()) {
        channel.sendPacketSync(packet);
      } else {
        channel.sendPacket(packet);
      }
    }

    // release the message now
    message.content().release();
  }

  public @NonNull CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NonNull ChannelMessage message,
    boolean allowClusterRedirect
  ) {
    // find the target channels to send the message to
    var channels = this.findChannels(message.targets(), allowClusterRedirect);
    if (channels.isEmpty()) {
      // no target channels found, release the message now
      message.content().release();
      return TaskUtil.finishedFuture(new HashSet<>());
    } else {
      // the result we generate
      Set<ChannelMessage> result = new HashSet<>();
      var task = new CountingTask<Collection<ChannelMessage>>(result, channels.size());

      // send the packet to each channel
      for (var channel : channels) {
        // acquire the message content for each channel we're sending the message to
        // when writing the message content to the target buffer, the message is released
        // that means when the message was written to all channels it's released unless someone acquired it before
        message.content().acquire();

        channel.sendQueryAsync(new ChannelMessagePacket(message, false)).whenComplete((packet, th) -> {
          // check if we got an actual result from the request
          if (th == null && packet.readable()) {
            // add all resulting messages we got
            result.addAll(packet.content().readObject(COL_MSG));
          }

          // count down - one channel responded
          task.countDown();
        });
      }

      // release the message now
      message.content().release();

      // return the task on which the user can wait
      return task;
    }
  }

  protected @NonNull Collection<NetworkChannel> findChannels(
    @NonNull Collection<ChannelMessageTarget> targets,
    boolean allowClusterRedirect
  ) {
    // check if there is only one channel
    if (targets.size() == 1) {
      // get the target - we can suppress the nullable warning because we expect the collection to not contain null values
      return this.findTargetChannels(Iterables.getOnlyElement(targets), allowClusterRedirect);
    } else {
      // filter all the channels for the targets
      return targets.stream()
        .flatMap(target -> this.findTargetChannels(target, allowClusterRedirect).stream())
        .collect(Collectors.toSet());
    }
  }

  protected @NonNull Collection<NetworkChannel> findTargetChannels(
    @NonNull ChannelMessageTarget target,
    boolean allowClusterRedirect
  ) {
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
      case NODE -> {
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
      case TASK -> {
        // lookup all services of the given task
        return this.filterChannels(
          this.cloudServiceManager.servicesByTask(target.name()),
          allowClusterRedirect);
      }
      case ENVIRONMENT -> {
        // lookup all services of the given environment
        return this.filterChannels(
          this.cloudServiceManager.servicesByEnvironment(target.environment().name()),
          allowClusterRedirect);
      }
      case GROUP -> {
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
