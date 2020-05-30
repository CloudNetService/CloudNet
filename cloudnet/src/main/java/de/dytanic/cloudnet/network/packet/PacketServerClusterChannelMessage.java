package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public final class PacketServerClusterChannelMessage extends Packet {

    public PacketServerClusterChannelMessage(String channel, String message, JsonDocument header, byte[] body) {
        super(
                PacketConstants.INTERNAL_PACKET_CLUSTER_MESSAGE_CHANNEL,
                JsonDocument.EMPTY,
                ProtocolBuffer.create()
                        .writeString(channel)
                        .writeString(message)
                        .writeString(header.toJson())
                        .writeArray(body)
                        .toArray()
        );
    }
}