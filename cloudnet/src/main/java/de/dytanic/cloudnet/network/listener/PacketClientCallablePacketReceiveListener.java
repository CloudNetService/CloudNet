package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;

public final class PacketClientCallablePacketReceiveListener implements
  IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    if (packet.getHeader().contains(PacketConstants.SYNC_PACKET_ID_PROPERTY)
      && packet.getHeader()
      .contains(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY)) {
      CloudNetDriver.getInstance().getTaskScheduler().schedule(new Runnable() {
        @Override
        public void run() {
          handle0(channel, packet);
        }
      });
    }
  }

  private void handle0(INetworkChannel channel, IPacket packet) {
    NetworkChannelReceiveCallablePacketEvent event = CloudNetDriver
      .getInstance().getEventManager()
      .callEvent(new NetworkChannelReceiveCallablePacketEvent(
        channel,
        packet.getUniqueId(),
        packet.getHeader()
          .getString(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY),
        packet.getHeader()
          .getString(PacketConstants.SYNC_PACKET_ID_PROPERTY),
        packet.getHeader()
      ));

    if (event.getCallbackPacket() != null) {
      channel.sendPacket(event.getCallbackPacket());
    }
  }
}