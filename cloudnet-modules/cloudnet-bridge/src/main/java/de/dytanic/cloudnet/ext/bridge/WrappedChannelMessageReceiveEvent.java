package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface WrappedChannelMessageReceiveEvent {

    ChannelMessageReceiveEvent getWrapped();

    @NotNull
    default ChannelMessageSender getSender() {
        return this.getWrapped().getSender();
    }

    @NotNull
    default Collection<ChannelMessageTarget> getTargets() {
        return this.getWrapped().getTargets();
    }

    @NotNull
    default String getChannel() {
        return this.getWrapped().getChannel();
    }

    @Nullable
    default String getMessage() {
        return this.getWrapped().getMessage();
    }

    @NotNull
    default ChannelMessage getChannelMessage() {
        return this.getWrapped().getChannelMessage();
    }

    @NotNull
    default JsonDocument getData() {
        return this.getWrapped().getData();
    }

    @NotNull
    default ProtocolBuffer getBuffer() {
        return this.getWrapped().getBuffer();
    }

    default boolean isQuery() {
        return this.getWrapped().isQuery();
    }

    default void setQueryResponse(@Nullable ChannelMessage queryResponse) {
        this.getWrapped().setQueryResponse(queryResponse);
    }

    default void setJsonResponse(@NotNull JsonDocument json) {
        this.getWrapped().setJsonResponse(json);
    }

    default void setBinaryResponse(@NotNull ProtocolBuffer buffer) {
        this.getWrapped().setBinaryResponse(buffer);
    }

    default ProtocolBuffer createBinaryResponse() {
        return this.getWrapped().createBinaryResponse();
    }

}
