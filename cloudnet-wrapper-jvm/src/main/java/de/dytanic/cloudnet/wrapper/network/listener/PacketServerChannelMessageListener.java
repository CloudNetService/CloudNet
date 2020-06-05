package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public final class PacketServerChannelMessageListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        ChannelMessage message = packet.getBuffer().readObject(ChannelMessage.class);
        boolean query = packet.getBuffer().readBoolean();

        ChannelMessageReceiveEvent event = new ChannelMessageReceiveEvent(message, query);
        CloudNetDriver.getInstance().getEventManager().callEvent(event);

        if (query) {
            event.setQueryResponse(ChannelMessage.buildResponseFor(message).json(JsonDocument.newDocument("response", "x")).buffer(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}).build());

            ProtocolBuffer buffer = ProtocolBuffer.create();
            buffer.writeBoolean(event.getQueryResponse() != null);
            if (event.getQueryResponse() != null) {
                buffer.writeObject(event.getQueryResponse());
            }
            channel.sendPacket(new Packet(-1, packet.getUniqueId(), JsonDocument.EMPTY, buffer));
        }
    }
}