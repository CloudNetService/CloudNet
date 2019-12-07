package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;

public final class PacketClientCallablePacketReceiveListener implements PacketListener {

    @Override
    public void handle(INetworkChannel channel, Packet packet) {
        if (packet.getHeader().contains(PacketConstants.SYNC_PACKET_ID_PROPERTY) && packet.getHeader().contains(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY)) {
            CloudNetDriver.getInstance().getTaskScheduler().schedule(() -> handle0(channel, packet));
        }
    }

    private void handle0(INetworkChannel channel, Packet packet) {
        NetworkChannelReceiveCallablePacketEvent event = CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkChannelReceiveCallablePacketEvent(
                channel,
                packet.getUniqueId(),
                packet.getHeader().getString(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY),
                packet.getHeader().getString(PacketConstants.SYNC_PACKET_ID_PROPERTY),
                packet.getHeader()
        ));

        if (event.getCallbackPacket() != null) {
            channel.sendPacket(event.getCallbackPacket());
        }
    }
}