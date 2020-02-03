package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.NetworkClusterNodeInfoUpdateEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.network.packet.PacketServerClusterNodeInfoUpdate;
import de.dytanic.cloudnet.service.ICloudService;

public final class PacketServerClusterNodeInfoUpdateListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains("clusterNodeInfoSnapshot")) {
            NetworkClusterNodeInfoSnapshot snapshot = packet.getHeader().get("clusterNodeInfoSnapshot", NetworkClusterNodeInfoSnapshot.TYPE);
            IClusterNodeServer clusterNodeServer = CloudNet.getInstance().getClusterNodeServerProvider().getNodeServer(snapshot.getNode().getUniqueId());

            clusterNodeServer.setNodeInfoSnapshot(snapshot);
            CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkClusterNodeInfoUpdateEvent(channel, snapshot));

            Packet packet1 = new PacketServerClusterNodeInfoUpdate(snapshot);
            for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
                if (cloudService.getNetworkChannel() != null) {
                    cloudService.getNetworkChannel().sendPacket(packet1);
                }
            }
        }
    }
}
}