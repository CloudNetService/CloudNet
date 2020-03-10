package de.dytanic.cloudnet.driver.event.events.channel;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;

/**
 * This event is being called whenever a channel message is received.
 * You can send channel messages with the methods in {@link CloudNetDriver#getMessenger()}.
 */
public final class ChannelMessageReceiveEvent extends Event {

    private final String channel;

    private final String message;

    private final JsonDocument data;

    public ChannelMessageReceiveEvent(String channel, String message, JsonDocument data) {
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