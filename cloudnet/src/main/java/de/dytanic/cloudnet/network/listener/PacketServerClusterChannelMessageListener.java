package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.event.cluster.NetworkClusterChannelMessageReceiveEvent;

public final class PacketServerClusterChannelMessageListener implements
    IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    CloudNetDriver.getInstance().getEventManager()
        .callEvent(new NetworkClusterChannelMessageReceiveEvent(
            channel,
            packet.getHeader().getString("channel"),
            packet.getHeader().getString("message"),
            packet.getHeader().getDocument("header"),
            packet.getBody()
        ));
  }
}