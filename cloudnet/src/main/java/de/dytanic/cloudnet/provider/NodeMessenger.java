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

package de.dytanic.cloudnet.provider;

import com.google.common.collect.Iterables;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.common.concurrent.CountingTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.DefaultMessenger;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.network.packet.PacketServerChannelMessage;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class NodeMessenger extends DefaultMessenger implements CloudMessenger {

  protected static final Type COL_MSG = TypeToken.getParameterized(Collection.class, ChannelMessage.class).getType();

  protected final ICloudServiceManager cloudServiceManager;
  protected final IClusterNodeServerProvider nodeServerProvider;

  public NodeMessenger(ICloudServiceManager cloudServiceManager, IClusterNodeServerProvider nodeServerProvider) {
    this.cloudServiceManager = cloudServiceManager;
    this.nodeServerProvider = nodeServerProvider;
  }

  @Override
  public void sendChannelMessage(@NotNull ChannelMessage message) {
    this.sendChannelMessage(message, true);
  }

  @Override
  public @NotNull ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NotNull ChannelMessage message) {
    return this.sendChannelMessageQueryAsync(message, true);
  }

  @Override
  public @NotNull Collection<ChannelMessage> sendChannelMessageQuery(@NotNull ChannelMessage channelMessage) {
    return this.sendChannelMessageQueryAsync(channelMessage).get(20, TimeUnit.SECONDS, Collections.emptyList());
  }

  public void sendChannelMessage(@NotNull ChannelMessage message, boolean allowClusterRedirect) {
    for (INetworkChannel channel : this.findChannels(message.getTargets(), allowClusterRedirect)) {
      channel.sendPacket(new PacketServerChannelMessage(message));
    }
  }

  public @NotNull ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NotNull ChannelMessage message,
    boolean allowClusterRedirect
  ) {
    // filter the channels we need to send the message to
    Collection<INetworkChannel> channels = this.findChannels(message.getTargets(), allowClusterRedirect);
    // the result we generate
    Set<ChannelMessage> result = new HashSet<>();
    CountingTask<Collection<ChannelMessage>> task = new CountingTask<>(result, channels.size());
    // send the packet to each channel
    for (INetworkChannel channel : channels) {
      channel.sendQueryAsync(new PacketServerChannelMessage(message)).onComplete(resultPacket -> {
        // check if we got an actual result from the request
        if (resultPacket != Packet.EMPTY) {
          // add all resulting messages we got
          result.addAll(resultPacket.getContent().readObject(COL_MSG));
        }
        // count down - one channel responded
        task.countDown();
      });
    }
    // return the task on which the user can wait
    return task;
  }

  protected @NotNull Collection<INetworkChannel> findChannels(
    @NotNull Collection<ChannelMessageTarget> targets,
    boolean allowClusterRedirect
  ) {
    // check if there is only one channel
    if (targets.size() == 1) {
      // get the target
      return this.findTargetChannels(Iterables.getOnlyElement(targets), allowClusterRedirect);
    } else {
      // filter all the channels for the targets
      return targets.stream()
        .flatMap(target -> this.findTargetChannels(target, allowClusterRedirect).stream())
        .collect(Collectors.toSet());
    }
  }

  protected @NotNull Collection<INetworkChannel> findTargetChannels(
    @NotNull ChannelMessageTarget target,
    boolean allowClusterRedirect
  ) {
    switch (target.getType()) {
      // just include all known channels
      case ALL: {
        Set<INetworkChannel> result = new HashSet<>();
        // all local services
        this.cloudServiceManager.getLocalCloudServices().stream()
          .map(ICloudService::getNetworkChannel)
          .filter(Objects::nonNull)
          .forEach(result::add);
        // all connected nodes
        if (allowClusterRedirect) {
          result.addAll(this.nodeServerProvider.getConnectedChannels());
        }
        return result;
      }
      case NODE: {
        // search for the matching node server
        if (allowClusterRedirect) {
          IClusterNodeServer server = this.nodeServerProvider.getNodeServer(target.getName());
          return server == null || !server.isConnected()
            ? Collections.emptySet()
            : Collections.singleton(server.getChannel());
        } else {
          // not allowed to redirect the message
          return Collections.emptySet();
        }
      }
      case SERVICE: {
        // check if the service is running locally - use the known channel then
        ICloudService localService = this.cloudServiceManager.getLocalCloudService(target.getName());
        if (localService != null) {
          return localService.getNetworkChannel() == null
            ? Collections.emptySet()
            : Collections.singleton(localService.getNetworkChannel());
        }
        // check if we are allowed to redirect the message to the node running the service
        if (allowClusterRedirect) {
          // check if we know the service from the cluster
          ServiceInfoSnapshot service = this.cloudServiceManager.getCloudServiceByName(target.getName());
          if (service != null) {
            // check if we know the target node server to send the channel message to instead
            IClusterNodeServer server = this.nodeServerProvider.getNodeServer(service.getServiceId().getNodeUniqueId());
            return server == null || !server.isConnected()
              ? Collections.emptySet()
              : Collections.singleton(server.getChannel());
          }
        }
        // unable to retrieve information about the target - just an empty set then
        return Collections.emptySet();
      }
      case TASK: {
        // lookup all services of the given task
        return this.filterChannels(
          this.cloudServiceManager.getCloudServices(target.getName()),
          allowClusterRedirect);
      }
      case ENVIRONMENT: {
        // lookup all services of the given environment
        return this.filterChannels(
          this.cloudServiceManager.getCloudServices(target.getEnvironment()),
          allowClusterRedirect);
      }
      case GROUP: {
        // lookup all services of the given group
        return this.filterChannels(
          this.cloudServiceManager.getCloudServicesByGroup(target.getName()),
          allowClusterRedirect);
      }
      default: {
        throw new IllegalArgumentException("Unhandled ChannelMessageTarget.Type: " + target.getType());
      }
    }
  }

  protected @NotNull Collection<INetworkChannel> filterChannels(
    @NotNull Collection<ServiceInfoSnapshot> snapshots,
    boolean allowClusterRedirect
  ) {
    return snapshots.stream()
      .map(service -> {
        // check if the service is running locally
        ICloudService localService = this.cloudServiceManager.getLocalCloudService(service.getServiceId().getName());
        if (localService != null) {
          return localService.getNetworkChannel();
        }
        // check if we are allowed to redirect the message to the node running the service
        if (allowClusterRedirect) {
          // check if we know the node on which the service is running
          IClusterNodeServer nodeServer = this.nodeServerProvider.getNodeServer(
            service.getServiceId().getNodeUniqueId());
          return nodeServer == null ? null : nodeServer.getChannel();
        }
        // no target found
        return null;
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }
}
