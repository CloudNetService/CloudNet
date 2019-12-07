package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;

public final class PacketServerClusterChannelMessage extends AbstractPacket {

    public PacketServerClusterChannelMessage(String channel, String message, JsonDocument header, byte[] body) {
        super(
                PacketConstants.INTERNAL_PACKET_CLUSTER_MESSAGE_CHANNEL,
                new JsonDocument("channel", channel).append("message", message).append("header", header),
                body
        );
    }
}