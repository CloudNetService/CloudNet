package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import de.dytanic.cloudnet.event.cluster.NetworkClusterChannelMessageReceiveEvent;

public final class PacketServerClusterChannelMessageListener implements PacketListener {

    @Override
    public void handle(INetworkChannel channel, Packet packet) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkClusterChannelMessageReceiveEvent(
                channel,
                packet.getHeader().getString("channel"),
                packet.getHeader().getString("message"),
                packet.getHeader().getDocument("header"),
                packet.getBody()
        ));
    }
}