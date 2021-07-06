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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.CountingTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.provider.DefaultMessenger;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

public class NodeMessenger extends DefaultMessenger implements CloudMessenger {

  private final CloudNet cloudNet;

  public NodeMessenger(CloudNet cloudNet) {
    this.cloudNet = cloudNet;
  }

  public Collection<ChannelMessageTargetChannel> getTargetChannels(ChannelMessageSender sender,
    Collection<ChannelMessageTarget> targets, boolean serviceOnly) {
    Collection<ChannelMessageTargetChannel> allChannels = new ArrayList<>();
    for (ChannelMessageTarget target : targets) {
      Collection<ChannelMessageTargetChannel> channels = this.getTargetChannels(sender, target, serviceOnly);
      if (channels != null) {
        allChannels.addAll(channels);
      }
    }
    return allChannels;
  }

  public Collection<ChannelMessageTargetChannel> getTargetChannels(ChannelMessageSender sender,
    ChannelMessageTarget target, boolean serviceOnly) {
    switch (target.getType()) {
      case NODE: {
        if (serviceOnly) {
          return null;
        }
        if (target.getName() == null) {
          Collection<ChannelMessageTargetChannel> channels = new ArrayList<>();
          for (IClusterNodeServer server : this.cloudNet.getClusterNodeServerProvider().getNodeServers()) {
            if (server.getChannel() != null) {
              channels.add(new ChannelMessageTargetChannel(server.getChannel(), true));
            }
          }
          return channels;
        }

        IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider().getNodeServer(target.getName());
        return server != null ? Collections.singletonList(new ChannelMessageTargetChannel(server.getChannel(), true))
          : null;
      }
      case TASK: {
        if (target.getName() == null) {
          return this.getAll(sender, serviceOnly);
        }
        Collection<ServiceInfoSnapshot> services = this.cloudNet.getCloudServiceProvider()
          .getCloudServices(target.getName());
        return this.getSendersFromServices(services, serviceOnly);
      }
      case GROUP: {
        if (target.getName() == null) {
          return this.getAll(sender, serviceOnly);
        }
        Collection<ServiceInfoSnapshot> services = this.cloudNet.getCloudServiceProvider()
          .getCloudServicesByGroup(target.getName());
        return this.getSendersFromServices(services, serviceOnly);
      }
      case SERVICE: {
        if (target.getName() == null) {
          return this.getAll(sender, serviceOnly);
        }
        ServiceInfoSnapshot service = this.cloudNet.getCloudServiceProvider().getCloudServiceByName(target.getName());
        if (service == null) {
          return null;
        }
        ICloudService localService = this.cloudNet.getCloudServiceManager()
          .getCloudService(service.getServiceId().getUniqueId());
        if (localService != null) {
          return localService.getNetworkChannel() != null ? Collections
            .singletonList(new ChannelMessageTargetChannel(localService.getNetworkChannel(), false)) : null;
        }
        if (serviceOnly) {
          return null;
        }
        IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider()
          .getNodeServer(service.getServiceId().getNodeUniqueId());
        return server != null && server.getChannel() != null ? Collections
          .singletonList(new ChannelMessageTargetChannel(server.getChannel(), true)) : null;
      }
      case ENVIRONMENT: {
        Collection<ServiceInfoSnapshot> services = this.cloudNet.getCloudServiceProvider()
          .getCloudServices(target.getEnvironment());
        return this.getSendersFromServices(services, serviceOnly);
      }
      case ALL: {
        return this.getAll(sender, serviceOnly);
      }
      default:
        break;
    }
    return null;
  }

  private Collection<ChannelMessageTargetChannel> getAll(ChannelMessageSender sender, boolean serviceOnly) {
    Collection<ChannelMessageTargetChannel> channels = new ArrayList<>();
    for (ICloudService localService : this.cloudNet.getCloudServiceManager().getLocalCloudServices()) {
      if (localService.getNetworkChannel() != null) {
        channels.add(new ChannelMessageTargetChannel(localService.getNetworkChannel(), false));
      }
    }
    if (!serviceOnly) {
      for (IClusterNodeServer server : this.cloudNet.getClusterNodeServerProvider().getNodeServers()) {
        if (server.getChannel() != null && !sender.isEqual(server.getNodeInfo())) {
          channels.add(new ChannelMessageTargetChannel(server.getChannel(), true));
        }
      }
    }
    return channels;
  }

  private Collection<ChannelMessageTargetChannel> getSendersFromServices(Collection<ServiceInfoSnapshot> services,
    boolean serviceOnly) {
    if (services.isEmpty()) {
      return Collections.emptyList();
    }
    Collection<ChannelMessageTargetChannel> channels = new ArrayList<>();
    for (ServiceInfoSnapshot service : services) {
      if (service.getServiceId().getNodeUniqueId().equals(this.cloudNet.getComponentName())) {
        ICloudService localService = this.cloudNet.getCloudServiceManager()
          .getCloudService(service.getServiceId().getUniqueId());
        if (localService != null && localService.getNetworkChannel() != null) {
          channels.add(new ChannelMessageTargetChannel(localService.getNetworkChannel(), false));
        }
      } else if (!serviceOnly) {
        IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider()
          .getNodeServer(service.getServiceId().getNodeUniqueId());
        if (server == null || server.getChannel() == null) {
          continue;
        }
        if (channels.stream().anyMatch(channel -> channel.getChannel().equals(server.getChannel()))) {
          continue;
        }
        channels.add(new ChannelMessageTargetChannel(server.getChannel(), true));
      }
    }
    return channels;
  }

  @Override
  public void sendChannelMessage(@NotNull ChannelMessage channelMessage) {
    this.sendChannelMessage(channelMessage, false);
  }

  public void sendChannelMessage(@NotNull ChannelMessage channelMessage, boolean serviceOnly) {
    if (channelMessage.getTargets().stream()
      .anyMatch(target -> target.includesNode(CloudNetDriver.getInstance().getComponentName()))) {
      channelMessage.getBuffer().markReaderIndex();
      CloudNetDriver.getInstance().getEventManager().callEvent(new ChannelMessageReceiveEvent(channelMessage, false));
      channelMessage.getBuffer().resetReaderIndex();
    }

    Collection<ChannelMessageTargetChannel> channels = this
      .getTargetChannels(channelMessage.getSender(), channelMessage.getTargets(), serviceOnly);
    if (channels == null || channels.isEmpty()) {
      return;
    }

    IPacket packet = new PacketClientServerChannelMessage(channelMessage, false);
    for (ChannelMessageTargetChannel channel : channels) {
      channel.getChannel().sendPacket(packet);
    }
  }

  @Override
  public @NotNull ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(
    @NotNull ChannelMessage channelMessage) {
    return this.sendChannelMessageQueryAsync(channelMessage, false);
  }

  public @NotNull ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NotNull ChannelMessage channelMessage,
    boolean serviceOnly) {
    Collection<ChannelMessage> result = new CopyOnWriteArrayList<>();

    if (channelMessage.getTargets().stream()
      .anyMatch(target -> target.includesNode(CloudNetDriver.getInstance().getComponentName()))) {
      channelMessage.getBuffer().markReaderIndex();

      ChannelMessage queryResponse = CloudNetDriver.getInstance().getEventManager()
        .callEvent(new ChannelMessageReceiveEvent(channelMessage, true)).getQueryResponse();
      if (queryResponse != null) {
        result.add(queryResponse);
      }

      channelMessage.getBuffer().resetReaderIndex();
    }

    Collection<ChannelMessageTargetChannel> channels = this
      .getTargetChannels(channelMessage.getSender(), channelMessage.getTargets(), serviceOnly);

    if (channels == null || channels.isEmpty()) {
      return CompletedTask.create(result);
    }

    CountingTask<Collection<ChannelMessage>> task = new CountingTask<>(result, channels.size());

    for (ChannelMessageTargetChannel channel : channels) {
      channelMessage.getBuffer().markReaderIndex();
      IPacket packet = new PacketClientServerChannelMessage(channelMessage, true);

      channel.getChannel().sendQueryAsync(packet).onComplete(response -> {
        if (response != null) {
          if (channel.isNode()) {
            result.addAll(response.getBuffer().readObjectCollection(ChannelMessage.class));
          } else if (response.getBuffer().readBoolean()) {
            result.add(response.getBuffer().readObject(ChannelMessage.class));
          }
        }
        task.countDown();
      });
      channelMessage.getBuffer().resetReaderIndex();
    }

    return task;
  }

  public static class ChannelMessageTargetChannel {

    private final INetworkChannel channel;

    private final boolean node;

    public ChannelMessageTargetChannel(INetworkChannel channel, boolean node) {
      this.channel = channel;
      this.node = node;
    }

    public INetworkChannel getChannel() {
      return this.channel;
    }

    public boolean isNode() {
      return this.node;
    }

  }

}
