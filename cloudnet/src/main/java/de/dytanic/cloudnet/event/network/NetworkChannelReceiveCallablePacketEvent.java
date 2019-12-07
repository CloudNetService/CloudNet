package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;

import java.util.UUID;

public final class NetworkChannelReceiveCallablePacketEvent extends NetworkEvent {

    private final String channelName;

    private final String id;

    private final JsonDocument header;

    private final UUID uniqueId;

    private Packet callbackPacket;

    public NetworkChannelReceiveCallablePacketEvent(INetworkChannel channel, UUID uniqueId, String channelName, String id, JsonDocument header) {
        super(channel);

        this.uniqueId = uniqueId;
        this.header = header;
        this.channelName = channelName;
        this.id = id;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public String getId() {
        return this.id;
    }

    public JsonDocument getHeader() {
        return this.header;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public Packet getCallbackPacket() {
        return this.callbackPacket;
    }

    public void setCallbackPacket(JsonDocument header) {
        this.callbackPacket = new AbstractPacket(PacketConstants.INTERNAL_CALLABLE_CHANNEL, this.uniqueId, header, null);
    }
}