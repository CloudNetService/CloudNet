package de.dytanic.cloudnet.driver.event.events.channel;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event is being called whenever a channel message is received.
 * You can send channel messages with the methods in {@link CloudNetDriver#getMessenger()}.
 */
public final class ChannelMessageReceiveEvent extends Event {

    private final ChannelMessage channelMessage;
    private final boolean query;

    private ChannelMessage queryResponse;

    public ChannelMessageReceiveEvent(@NotNull ChannelMessage message, boolean query) {
        this.channelMessage = message;
        this.query = query;
    }

    @NotNull
    public String getChannel() {
        return this.channelMessage.getChannel();
    }

    @Nullable
    public String getMessage() {
        return this.channelMessage.getMessage();
    }

    @NotNull
    public ChannelMessage getChannelMessage() {
        return this.channelMessage;
    }

    @NotNull
    public JsonDocument getData() {
        return this.channelMessage.getJson();
    }

    @NotNull
    public ProtocolBuffer getBuffer() {
        return this.channelMessage.getBuffer();
    }

    public boolean isQuery() {
        return this.query;
    }

    public void setQueryResponse(@Nullable ChannelMessage queryResponse) {
        Preconditions.checkArgument(this.query, "Cannot set query response of no query");
        this.queryResponse = queryResponse;
    }

    public void setJsonResponse(@NotNull JsonDocument json) {
        this.setQueryResponse(ChannelMessage.buildResponseFor(this.channelMessage).json(json).build());
    }

    public void setBinaryResponse(@NotNull ProtocolBuffer buffer) {
        this.setQueryResponse(ChannelMessage.buildResponseFor(this.channelMessage).buffer(buffer).build());
    }

    @Nullable
    public ChannelMessage getQueryResponse() {
        return this.queryResponse;
    }
}