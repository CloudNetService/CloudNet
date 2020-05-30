package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;

import java.util.UUID;

public final class PacketClientServerChannelMessage extends Packet {

    public PacketClientServerChannelMessage(String channel, String message, JsonDocument data) {
        super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument("channel", channel).append("message", message).append("data", data));
    }

    public PacketClientServerChannelMessage(UUID targetServiceId, String channel, String message, JsonDocument data) {
        super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument("channel", channel).append("message", message).append("data", data).append("uniqueId", targetServiceId));
    }

    public PacketClientServerChannelMessage(String taskName, String channel, String message, JsonDocument data) {
        super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument("channel", channel).append("message", message).append("data", data).append("task", taskName));
    }

    public PacketClientServerChannelMessage(ServiceEnvironmentType environment, String channel, String message, JsonDocument data) {
        super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL, new JsonDocument("channel", channel).append("message", message).append("data", data).append("environment", environment));
    }

}