package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public final class PacketClientServerChannelMessage extends Packet {

    public PacketClientServerChannelMessage(ChannelMessage message, boolean query) {
        super(PacketConstants.CHANNEL_MESSAGING_CHANNEL, ProtocolBuffer.create().writeObject(message).writeBoolean(query));
    }

}