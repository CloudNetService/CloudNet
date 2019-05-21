package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import lombok.Getter;

import java.util.UUID;

@Getter
public final class NetworkChannelReceiveCallablePacketEvent extends NetworkEvent {

    private final String channelName;

    private final String id;

    private final JsonDocument header;

    private final UUID uniqueId;

    private IPacket callbackPacket;

    public NetworkChannelReceiveCallablePacketEvent(INetworkChannel channel, UUID uniqueId, String channelName, String id, JsonDocument header)
    {
        super(channel);

        this.uniqueId = uniqueId;
        this.header = header;
        this.channelName = channelName;
        this.id = id;
    }

    public void setCallbackPacket(JsonDocument header)
    {
        this.callbackPacket = new Packet(PacketConstants.INTERNAL_CALLABLE_CHANNEL, this.uniqueId, header, null);
    }
}