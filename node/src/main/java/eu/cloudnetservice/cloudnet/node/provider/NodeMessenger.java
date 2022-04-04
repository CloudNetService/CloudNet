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

package eu.cloudnetservice.cloudnet.node.provider;

import com.google.common.collect.Iterables;
import com.google.gson.reflect.TypeToken;
import eu.cloudnetservice.cloudnet.common.concurrent.CountingTask;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.def.PacketServerChannelMessage;
import eu.cloudnetservice.cloudnet.driver.provider.CloudMessenger;
import eu.cloudnetservice.cloudnet.driver.provider.defaults.DefaultMessenger;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.node.Node;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServerProvider;
import eu.cloudnetservice.cloudnet.node.service.CloudService;
import eu.cloudnetservice.cloudnet.node.service.CloudServiceManager;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.NonNull;

public class NodeMessenger extends DefaultMessenger implements CloudMessenger {

  protected static final Type COL_MSG = TypeToken.getParameterized(Collection.class, ChannelMessage.class).getType();

  protected final NodeServerProvider nodeServerProvider;
  protected final CloudServiceManager cloudServiceManager;

  public NodeMessenger(@NonNull Node nodeInstance) {
    this.nodeServerProvider = nodeInstance.nodeServerProvider();
    this.cloudServiceManager = nodeInstance.cloudServiceProvider();
  }

  @Override
  public void sendChannelMessage(@NonNull ChannelMessage message) {
    this.sendChannelMessage(message, true);
  }

  @Override
  public @NonNull Task<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NonNull ChannelMessage message) {
    return this.sendChannelMessageQueryAsync(message, true);
  }

  @Override
  public @NonNull Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage) {
    return this.sendChannelMessageQueryAsync(channelMessage).get(20, TimeUnit.SECONDS, Collections.emptyList());
  }

  public void sendChannelMessage(@NonNull ChannelMessage message, boolean allowClusterRedirect) {
    for (var channel : this.findChannels(message.targets(), allowClusterRedirect)) {
      if (message.sendSync()) {
        channel.sendPacketSync(new PacketServerChannelMessage(message, false));
      } else {
        channel.sendPacket(new PacketServerChannelMessage(message, false));
      }
    }
  }

  public @NonNull Task<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NonNull ChannelMessage message,
    boolean allowClusterRedirect
  ) {
    // filter the channels we need to send the message to
    var channels = this.findChannels(message.targets(), allowClusterRedirect);
    // the result we generate
    Set<ChannelMessage> result = new HashSet<>();
    var task = new CountingTask<Collection<ChannelMessage>>(result, channels.size());
    // send the packet to each channel
    for (var channel : channels) {
      channel.sendQueryAsync(new PacketServerChannelMessage(message, false)).whenComplete((packet, th) -> {
        // check if we got an actual result from the request
        if (th == null && packet.readable()) {
          // add all resulting messages we got
          result.addAll(packet.content().readObject(COL_MSG));
        }
        // count down - one channel responded
        task.countDown();
      });
    }
    // return the task on which the user can wait
    return task;
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
          Collection<NetworkChannel> channels = this.cloudServiceManager.localCloudServices().stream()
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
