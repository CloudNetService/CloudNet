package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;

import java.util.UUID;

public final class PacketClientServerChannelMessage extends AbstractPacket {

    public PacketClientServerChannelMessage(String channel, String message, JsonDocument data) {
        super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument("channel", channel).append("message", message).append("data", data), null);
    }

    public PacketClientServerChannelMessage(UUID targetServiceId, String channel, String message, JsonDocument data) {
        super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument("channel", channel).append("message", message).append("data", data).append("uniqueId", targetServiceId), null);
    }

    public PacketClientServerChannelMessage(String taskName, String channel, String message, JsonDocument data) {
        super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument("channel", channel).append("message", message).append("data", data).append("task", taskName), null);
    }
}