package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.service.ICloudService;

public final class PacketServerChannelMessageNodeListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (packet.getHeader().contains("channel") && packet.getHeader().contains("message") && packet.getHeader().contains("data")) {
            Packet packetClientServerChannelMessage = new PacketClientServerChannelMessage(
                    packet.getHeader().getString("channel"),
                    packet.getHeader().getString("message"),
                    packet.getHeader().getDocument("data")
            );

            for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
                if (cloudService.getNetworkChannel() != null) {
                    cloudService.getNetworkChannel().sendPacket(packetClientServerChannelMessage);
                }
            }

            CloudNetDriver.getInstance().getEventManager().callEvent(
                    new ChannelMessageReceiveEvent(
                            packet.getHeader().getString("channel"),
                            packet.getHeader().getString("message"),
                            packet.getHeader().getDocument("data")
                    ));
        }
    }
}