package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;

public final class PacketServerChannelMessageListener implements PacketListener {

    @Override
    public void handle(INetworkChannel channel, Packet packet) {
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