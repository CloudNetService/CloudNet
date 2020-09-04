package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class NodeMessenger extends DefaultMessenger implements CloudMessenger {
    private final CloudNet cloudNet;

    public NodeMessenger(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    public Collection<INetworkChannel> getTargetChannels(ChannelMessageSender sender, Collection<ChannelMessageTarget> targets, boolean serviceOnly) {
        Collection<INetworkChannel> allChannels = new ArrayList<>();
        for (ChannelMessageTarget target : targets) {
            Collection<INetworkChannel> channels = this.getTargetChannels(sender, target, serviceOnly);
            if (channels != null) {
                allChannels.addAll(channels);
            }
        }
        return allChannels;
    }

    public Collection<INetworkChannel> getTargetChannels(ChannelMessageSender sender, ChannelMessageTarget target, boolean serviceOnly) {
        switch (target.getType()) {
            case NODE: {
                if (serviceOnly) {
                    return null;
                }
                if (target.getName() == null) {
                    Collection<INetworkChannel> channels = new ArrayList<>();
                    for (IClusterNodeServer server : this.cloudNet.getClusterNodeServerProvider().getNodeServers()) {
                        if (server.getChannel() != null) {
                            channels.add(server.getChannel());
                        }
                    }
                    return channels;
                }

                IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider().getNodeServer(target.getName());
                return server != null ? Collections.singletonList(server.getChannel()) : null;
            }
            case TASK: {
                if (target.getName() == null) {
                    return this.getAll(sender, serviceOnly);
                }
                Collection<ServiceInfoSnapshot> services = this.cloudNet.getCloudServiceProvider().getCloudServices(target.getName());
                return this.getSendersFromServices(sender, services, serviceOnly);
            }
            case GROUP: {
                if (target.getName() == null) {
                    return this.getAll(sender, serviceOnly);
                }
                Collection<ServiceInfoSnapshot> services = this.cloudNet.getCloudServiceProvider().getCloudServicesByGroup(target.getName());
                return this.getSendersFromServices(sender, services, serviceOnly);
            }
            case SERVICE: {
                if (target.getName() == null) {
                    return this.getAll(sender, serviceOnly);
                }
                ServiceInfoSnapshot service = this.cloudNet.getCloudServiceProvider().getCloudServiceByName(target.getName());
                if (service == null) {
                    return null;
                }
                ICloudService localService = this.cloudNet.getCloudServiceManager().getCloudService(service.getServiceId().getUniqueId());
                if (localService != null) {
                    return localService.getNetworkChannel() != null ? Collections.singletonList(localService.getNetworkChannel()) : null;
                }
                if (serviceOnly) {
                    return null;
                }
                IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider().getNodeServer(service.getServiceId().getNodeUniqueId());
                return server != null && server.getChannel() != null ? Collections.singletonList(server.getChannel()) : null;
            }
            case ENVIRONMENT: {
                Collection<ServiceInfoSnapshot> services = this.cloudNet.getCloudServiceProvider().getCloudServices(target.getEnvironment());
                return this.getSendersFromServices(sender, services, serviceOnly);
            }
            case ALL: {
                return this.getAll(sender, serviceOnly);
            }
        }
        return null;
    }

    private Collection<INetworkChannel> getAll(ChannelMessageSender sender, boolean serviceOnly) {
        Collection<INetworkChannel> channels = new ArrayList<>();
        for (ICloudService localService : this.cloudNet.getCloudServiceManager().getLocalCloudServices()) {
            if (localService.getNetworkChannel() != null) {
                channels.add(localService.getNetworkChannel());
            }
        }
        if (!serviceOnly) {
            for (IClusterNodeServer server : this.cloudNet.getClusterNodeServerProvider().getNodeServers()) {
                if (server.getChannel() != null && !sender.isEqual(server.getNodeInfo())) {
                    channels.add(server.getChannel());
                }
            }
        }
        return channels;
    }

    private Collection<INetworkChannel> getSendersFromServices(ChannelMessageSender sender, Collection<ServiceInfoSnapshot> services, boolean serviceOnly) {
        if (services.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<INetworkChannel> channels = new ArrayList<>();
        for (ServiceInfoSnapshot service : services) {
            if (service.getServiceId().getNodeUniqueId().equals(this.cloudNet.getComponentName())) {
                ICloudService localService = this.cloudNet.getCloudServiceManager().getCloudService(service.getServiceId().getUniqueId());
                if (localService != null && localService.getNetworkChannel() != null) {
                    channels.add(localService.getNetworkChannel());
                }
            } else if (!serviceOnly) {
                IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider().getNodeServer(service.getServiceId().getNodeUniqueId());
                if (server == null || server.getChannel() == null) {
                    continue;
                }
                if (channels.contains(server.getChannel())) {
                    continue;
                }
                channels.add(server.getChannel());
            }
        }
        return channels;
    }

    @Override
    public void sendChannelMessage(@NotNull ChannelMessage channelMessage) {
        if (channelMessage.getTargets().stream().anyMatch(target -> target.includesNode(CloudNetDriver.getInstance().getComponentName()))) {
            channelMessage.getBuffer().markReaderIndex();
            CloudNetDriver.getInstance().getEventManager().callEvent(new ChannelMessageReceiveEvent(channelMessage, false));
            channelMessage.getBuffer().resetReaderIndex();
        }

        Collection<INetworkChannel> channels = this.getTargetChannels(channelMessage.getSender(), channelMessage.getTargets(), false);
        if (channels == null || channels.isEmpty()) {
            return;
        }

        IPacket packet = new PacketClientServerChannelMessage(channelMessage, false);
        for (INetworkChannel channel : channels) {
            channel.sendPacket(packet);
        }
    }

    @Override
    public @NotNull ITask<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NotNull ChannelMessage channelMessage) {
        Collection<INetworkChannel> channels = this.getTargetChannels(channelMessage.getSender(), channelMessage.getTargets(), false);
        boolean includesSelf = channelMessage.getTargets().stream().anyMatch(target -> target.includesNode(CloudNetDriver.getInstance().getComponentName()));

        if ((channels == null || channels.isEmpty()) && !includesSelf) {
            return CompletedTask.create(Collections.emptyList());
        }

        return this.cloudNet.scheduleTask(() -> {
            Collection<ChannelMessage> result = new ArrayList<>();
            IPacket packet = new PacketClientServerChannelMessage(channelMessage, true);

            if (channels != null) {
                for (INetworkChannel channel : channels) {
                    IPacket response = channel.sendQuery(packet);
                    if (response != null && response.getBuffer().readBoolean()) {
                        result.add(response.getBuffer().readObject(ChannelMessage.class));
                    }
                }
            }

            if (includesSelf) {
                ChannelMessage queryResponse = CloudNetDriver.getInstance().getEventManager().callEvent(new ChannelMessageReceiveEvent(channelMessage, true)).getQueryResponse();
                if (queryResponse != null) {
                    result.add(queryResponse);
                }
            }

            return result;
        });
    }

}
