package de.dytanic.cloudnet.network;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelInitEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.ArrayList;
import java.util.Collection;

final class NetworkChannelHandlerUtils {

    private NetworkChannelHandlerUtils() {
        throw new UnsupportedOperationException();
    }

    static boolean handleInitChannel(INetworkChannel channel, ChannelType channelType) {
        NetworkChannelInitEvent networkChannelInitEvent = new NetworkChannelInitEvent(channel, channelType);
        CloudNetDriver.getInstance().getEventManager().callEvent(networkChannelInitEvent);

        if (networkChannelInitEvent.isCancelled()) {
            try {
                channel.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return false;
        }

        return true;
    }

    static void handleRemoveDisconnectedClusterInNetwork(INetworkChannel channel, IClusterNodeServer clusterNodeServer) {
        try {
            clusterNodeServer.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        Collection<Packet> removed = new ArrayList<>();

        for (ServiceInfoSnapshot snapshot : CloudNet.getInstance().getCloudServiceProvider().getCloudServices()) {
            if (snapshot.getServiceId().getNodeUniqueId().equalsIgnoreCase(clusterNodeServer.getNodeInfo().getUniqueId())) {
                CloudNet.getInstance().getCloudServiceManager().getGlobalServiceInfoSnapshots().remove(snapshot.getServiceId().getUniqueId());
                removed.add(new PacketClientServerServiceInfoPublisher(snapshot, PacketClientServerServiceInfoPublisher.PublisherType.UNREGISTER));
                CloudNet.getInstance().getEventManager().callEvent(new CloudServiceUnregisterEvent(snapshot));
            }
        }

        for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                for (Packet packet : removed) {
                    cloudService.getNetworkChannel().sendPacket(packet);
                }
            }
        }

        System.out.println(LanguageManager.getMessage("cluster-server-networking-disconnected")
                .replace("%id%", clusterNodeServer.getNodeInfo().getUniqueId())
                .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
        );
    }
}