package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.events.network.NetworkEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;

public class NetworkClusterChannelMessageReceiveEvent extends NetworkEvent {

    private final String messageChannel, message;

    private final JsonDocument header;

    private final byte[] body;

    public NetworkClusterChannelMessageReceiveEvent(INetworkChannel networkChannel,
                                                    String channel, String message, JsonDocument header, byte[] body) {
        super(networkChannel);

        this.messageChannel = channel;
        this.message = message;
        this.header = header;
        this.body = body;
    }

    public String getMessageChannel() {
        return this.messageChannel;
    }

    public String getMessage() {
        return this.message;
    }

    public JsonDocument getHeader() {
        return this.header;
    }

    public byte[] getBody() {
        return this.body;
    }
}