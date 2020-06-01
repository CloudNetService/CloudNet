package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class NodeMessenger implements CloudMessenger {
    private final CloudNet cloudNet;

    public NodeMessenger(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    public Collection<INetworkChannel> getTargetChannels(ChannelMessageTarget target, boolean serviceOnly) {
        switch (target.getType()) {
            case NODE: {
                if (serviceOnly) {
                    return null;
                }
                IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider().getNodeServer(target.getName());
                return server != null ? Collections.singletonList(server.getChannel()) : null;
            }
            case TASK: {
                Collection<ServiceInfoSnapshot> services = this.cloudNet.getCloudServiceProvider().getCloudServices(target.getName());
                return this.getSendersFromServices(services, serviceOnly);
            }
            case GROUP: {
                Collection<ServiceInfoSnapshot> services = this.cloudNet.getCloudServiceProvider().getCloudServicesByGroup(target.getName());
                return this.getSendersFromServices(services, serviceOnly);
            }
            case SERVICE: {
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
                return this.getSendersFromServices(services, serviceOnly);
            }
            case ALL: {
                Collection<INetworkChannel> channels = new ArrayList<>();
                for (ICloudService localService : this.cloudNet.getCloudServiceManager().getLocalCloudServices()) {
                    if (localService.getNetworkChannel() != null) {
                        channels.add(localService.getNetworkChannel());
                    }
                }
                if (!serviceOnly) {
                    for (IClusterNodeServer server : this.cloudNet.getClusterNodeServerProvider().getNodeServers()) {
                        if (server.getChannel() != null) {
                            channels.add(server.getChannel());
                        }
                    }
                }
                return channels;
            }
        }
        return null;
    }

    private Collection<INetworkChannel> getSendersFromServices(Collection<ServiceInfoSnapshot> services, boolean serviceOnly) {
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
        Collection<INetworkChannel> channels = this.getTargetChannels(channelMessage.getTarget(), false);
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
        throw new UnsupportedOperationException("not implemented yet"); // TODO
    }

    @Override
    public @NotNull Collection<ChannelMessage> sendChannelMessageQuery(@NotNull ChannelMessage channelMessage) {
        return this.sendChannelMessageQueryAsync(channelMessage).get(10, TimeUnit.SECONDS, Collections.emptyList());
    }
}
