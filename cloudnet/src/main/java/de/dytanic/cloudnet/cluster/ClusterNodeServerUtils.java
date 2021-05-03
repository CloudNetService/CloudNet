package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.ArrayList;
import java.util.Collection;

final class ClusterNodeServerUtils {

    private ClusterNodeServerUtils() {
        throw new UnsupportedOperationException();
    }

    public static void handleNodeServerClose(INetworkChannel channel, IClusterNodeServer server) {
        Collection<Packet> removed = new ArrayList<>();

        for (ServiceInfoSnapshot snapshot : CloudNet.getInstance().getCloudServiceProvider().getCloudServices()) {
            if (snapshot.getServiceId().getNodeUniqueId().equalsIgnoreCase(server.getNodeInfo().getUniqueId())) {
                CloudNet.getInstance().getCloudServiceManager().handleServiceUpdate(PacketClientServerServiceInfoPublisher.PublisherType.UNREGISTER, snapshot);
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
                .replace("%id%", server.getNodeInfo().getUniqueId())
                .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
        );
    }
}
