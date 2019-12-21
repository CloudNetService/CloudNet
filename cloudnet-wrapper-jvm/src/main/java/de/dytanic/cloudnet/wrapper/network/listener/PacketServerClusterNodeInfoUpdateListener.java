package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.NetworkClusterNodeInfoUpdateEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;

public final class PacketServerClusterNodeInfoUpdateListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains("clusterNodeInfoSnapshot")) {
            NetworkClusterNodeInfoSnapshot snapshot = packet.getHeader().get("clusterNodeInfoSnapshot", NetworkClusterNodeInfoSnapshot.TYPE);
            CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkClusterNodeInfoUpdateEvent(channel, snapshot));
        }
    }
}