package de.dytanic.cloudnet.network;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelCloseEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.def.internal.InternalSyncPacketChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.service.ICloudService;

public final class NetworkServerChannelHandlerImpl implements INetworkChannelHandler {

    @Override
    public void handleChannelInitialize(INetworkChannel channel) {
        //Whitelist check
        if (!inWhitelist(channel)) {
            try {
                channel.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return;
        }

        if (!NetworkChannelHandlerUtils.handleInitChannel(channel, ChannelType.SERVER_CHANNEL)) {
            return;
        }

        System.out.println(LanguageManager.getMessage("server-network-channel-init")
                .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
        );
    }

    @Override
    public boolean handlePacketReceive(INetworkChannel channel, Packet packet) {
        if (InternalSyncPacketChannel.handleIncomingChannel(packet)) {
            return false;
        }

        return !CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkChannelPacketReceiveEvent(channel, packet)).isCancelled();
    }

    @Override
    public void handleChannelClose(INetworkChannel channel) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkChannelCloseEvent(channel, ChannelType.SERVER_CHANNEL));

        System.out.println(LanguageManager.getMessage("server-network-channel-close")
                .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
        );

        ICloudService cloudService = CloudNet.getInstance().getCloudServiceManager().getCloudService(iCloudService -> iCloudService.getNetworkChannel() != null && iCloudService.getNetworkChannel().equals(channel));

        if (cloudService != null) {
            closeAsCloudService(cloudService, channel);
            return;
        }

        IClusterNodeServer clusterNodeServer = CloudNet.getInstance().getClusterNodeServerProvider().getNodeServer(channel);

        if (clusterNodeServer != null) {
            NetworkChannelHandlerUtils.handleRemoveDisconnectedClusterInNetwork(channel, clusterNodeServer);
        }
    }

    private void closeAsCloudService(ICloudService cloudService, INetworkChannel channel) {
        cloudService.setNetworkChannel(null);
        System.out.println(LanguageManager.getMessage("cloud-service-networking-disconnected")
                .replace("%id%", cloudService.getServiceId().getUniqueId().toString())
                .replace("%task%", cloudService.getServiceId().getTaskName())
                .replace("%serviceId%", String.valueOf(cloudService.getServiceId().getTaskServiceId()))
                .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
        );

        CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(cloudService.getServiceInfoSnapshot(), PacketClientServerServiceInfoPublisher.PublisherType.DISCONNECTED));
    }

    private boolean inWhitelist(INetworkChannel channel) {
        for (String whitelistAddress : CloudNet.getInstance().getConfig().getIpWhitelist()) {
            if (channel.getClientAddress().getHost().equals(whitelistAddress)) {
                return true;
            }
        }

        return false;
    }

}