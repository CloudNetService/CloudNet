package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class BungeeChannelMessageReceiveEvent extends BungeeCloudNetEvent {

    private final ChannelMessageReceiveEvent event;

    public BungeeChannelMessageReceiveEvent(ChannelMessageReceiveEvent event) {
        this.event = event;
    }

    @NotNull
    public ChannelMessageSender getSender() {
        return this.event.getSender();
    }

    @NotNull
    public Collection<ChannelMessageTarget> getTargets() {
        return this.event.getTargets();
    }

    @NotNull
    public String getChannel() {
        return this.event.getChannel();
    }

    @Nullable
    public String getMessage() {
        return this.event.getMessage();
    }

    @NotNull
    public ChannelMessage getChannelMessage() {
        return this.event.getChannelMessage();
    }

    @NotNull
    public JsonDocument getData() {
        return this.event.getData();
    }

    @NotNull
    public ProtocolBuffer getBuffer() {
        return this.event.getBuffer();
    }

    public boolean isQuery() {
        return this.event.isQuery();
    }

    public void setQueryResponse(@Nullable ChannelMessage queryResponse) {
        this.event.setQueryResponse(queryResponse);
    }

    public void setJsonResponse(@NotNull JsonDocument json) {
        this.event.setJsonResponse(json);
    }

    public void setBinaryResponse(@NotNull ProtocolBuffer buffer) {
        this.event.setBinaryResponse(buffer);
    }

    public ProtocolBuffer createBinaryResponse() {
        return this.event.createBinaryResponse();
    }

}