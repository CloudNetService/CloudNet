package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;

public final class PacketServerChannelMessageListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains("channel") && packet.getHeader().contains("message") && packet.getHeader().contains("data")) {
            CloudNetDriver.getInstance().getEventManager().callEvent(
                    new ChannelMessageReceiveEvent(
                            packet.getHeader().getString("channel"),
                            packet.getHeader().getString("message"),
                            packet.getHeader().getDocument("data")
                    ));
        }
    }
}