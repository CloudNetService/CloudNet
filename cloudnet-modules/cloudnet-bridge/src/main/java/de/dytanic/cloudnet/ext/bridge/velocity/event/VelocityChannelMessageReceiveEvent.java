package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

public final class VelocityChannelMessageReceiveEvent extends VelocityCloudNetEvent {

    private final String channel, message;

    private final JsonDocument data;

    public VelocityChannelMessageReceiveEvent(String channel, String message, JsonDocument data) {
        this.channel = channel;
        this.message = message;
        this.data = data;
    }

    public String getChannel() {
        return this.channel;
    }

    public String getMessage() {
        return this.message;
    }

    public JsonDocument getData() {
        return this.data;
    }
}