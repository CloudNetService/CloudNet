package de.dytanic.cloudnet.driver.channel;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@ToString
@EqualsAndHashCode
public class ChannelMessage implements SerializableObject {

    private ChannelMessageSender sender;
    private String channel;
    private String message;
    private JsonDocument header;
    private ProtocolBuffer body;
    private ChannelMessageTarget target;

    private ChannelMessage(@NotNull ChannelMessageSender sender) {
        this.sender = sender;
    }

    public ChannelMessage() {
    }

    @NotNull
    public ChannelMessageSender getSender() {
        return this.sender;
    }

    @NotNull
    public String getChannel() {
        return this.channel;
    }

    @Nullable
    public String getMessage() {
        return this.message;
    }

    @Nullable
    public JsonDocument getHeader() {
        return this.header;
    }

    @Nullable
    public ProtocolBuffer getBody() {
        return this.body;
    }

    @NotNull
    public ChannelMessageTarget getTarget() {
        return this.target;
    }

    public void send() {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(this);
    }

    @NotNull
    public ITask<Collection<ChannelMessage>> sendQueryAsync() {
        return CloudNetDriver.getInstance().getMessenger().sendChannelMessageQueryAsync(this);
    }

    @NotNull
    public Collection<ChannelMessage> sendQuery() {
        return CloudNetDriver.getInstance().getMessenger().sendChannelMessageQuery(this);
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeObject(this.sender);
        buffer.writeString(this.channel);
        buffer.writeOptionalString(this.message);
        buffer.writeOptionalString(this.header != null ? this.header.toJson() : null);
        buffer.writeOptionalArray(this.body != null ? this.body.toArray() : null);
        buffer.writeObject(this.target);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.sender = buffer.readObject(ChannelMessageSender.class);
        this.channel = buffer.readString();
        this.message = buffer.readOptionalString();
        String headerJson = buffer.readOptionalString();
        this.header = headerJson != null ? JsonDocument.newDocument(headerJson) : null;
        byte[] body = buffer.readOptionalArray();
        this.body = body != null ? ProtocolBuffer.wrap(body) : null;
        this.target = buffer.readObject(ChannelMessageTarget.class);
    }



    public static Builder builder() {
        return new Builder();
    }

    public static Builder buildResponseFor(@NotNull ChannelMessage input) {
        return builder().target(input.sender.getType() == DriverEnvironment.CLOUDNET ? ChannelMessageTarget.Type.NODE : ChannelMessageTarget.Type.SERVICE, input.sender.getName());
    }

    public static class Builder {

        private final ChannelMessage channelMessage;

        private Builder() {
            this.channelMessage = new ChannelMessage(ChannelMessageSender.self());
        }

        public Builder channel(@NotNull String channel) {
            this.channelMessage.channel = channel;
            return this;
        }

        public Builder message(@Nullable String message) {
            this.channelMessage.message = message;
            return this;
        }

        public Builder content(@Nullable JsonDocument document) {
            this.channelMessage.header = document;
            return this;
        }

        public Builder content(@Nullable byte[] bytes) {
            return this.content(bytes == null ? null : ProtocolBuffer.wrap(bytes));
        }

        public Builder content(@Nullable ProtocolBuffer buffer) {
            this.channelMessage.body = buffer;
            return this;
        }

        public Builder target(@NotNull ChannelMessageTarget target) {
            this.channelMessage.target = target;
            return this;
        }

        public Builder target(@NotNull ChannelMessageTarget.Type type, @Nullable String name) {
            return this.target(new ChannelMessageTarget(type, name));
        }

        public Builder targetAll(@NotNull ChannelMessageTarget.Type type) {
            return this.target(type, null);
        }

        public Builder targetAll() {
            return this.target(ChannelMessageTarget.Type.ALL, null);
        }

        public Builder targetService(@Nullable String name) {
            return this.target(ChannelMessageTarget.Type.SERVICE, name);
        }

        public Builder targetTask(@Nullable String name) {
            return this.target(ChannelMessageTarget.Type.TASK, name);
        }

        public Builder targetNode(@Nullable String name) {
            return this.target(ChannelMessageTarget.Type.NODE, name);
        }

        public Builder targetEnvironment(@NotNull ServiceEnvironmentType environment) {
            return this.target(new ChannelMessageTarget(environment));
        }

        public ChannelMessage build() {
            Preconditions.checkNotNull(this.channelMessage.channel, "No channel provided");
            Preconditions.checkNotNull(this.channelMessage.target, "No target provided");
            Preconditions.checkNotNull(this.channelMessage.target.getType(), "No type for the target provided");
            return this.channelMessage;
        }

    }

}
