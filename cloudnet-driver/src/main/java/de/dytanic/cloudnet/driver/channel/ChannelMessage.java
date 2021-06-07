/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.channel;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public class ChannelMessage implements SerializableObject {

  private ChannelMessageSender sender;
  private String channel;
  private String message;
  private JsonDocument json = JsonDocument.EMPTY;
  private ProtocolBuffer buffer = ProtocolBuffer.EMPTY;
  private Collection<ChannelMessageTarget> targets;

  private ChannelMessage(@NotNull ChannelMessageSender sender) {
    this.sender = sender;
    this.targets = new ArrayList<>();
  }

  public ChannelMessage() {
  }

  public static Builder builder() {
    return builder(ChannelMessageSender.self());
  }

  public static Builder builder(ChannelMessageSender sender) {
    return new Builder(sender);
  }

  public static Builder buildResponseFor(@NotNull ChannelMessage input) {
    return builder().channel("").target(input.sender.getType(), input.sender.getName());
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

  @NotNull
  public JsonDocument getJson() {
    return this.json;
  }

  @NotNull
  public ProtocolBuffer getBuffer() {
    return this.buffer;
  }

  @NotNull
  public Collection<ChannelMessageTarget> getTargets() {
    return this.targets;
  }

  public void send() {
    this.getMessenger().sendChannelMessage(this);
  }

  @NotNull
  public ITask<Collection<ChannelMessage>> sendQueryAsync() {
    return this.getMessenger().sendChannelMessageQueryAsync(this);
  }

  @NotNull
  public ITask<ChannelMessage> sendSingleQueryAsync() {
    return this.getMessenger().sendSingleChannelMessageQueryAsync(this);
  }

  @NotNull
  public Collection<ChannelMessage> sendQuery() {
    return this.getMessenger().sendChannelMessageQuery(this);
  }

  @Nullable
  public ChannelMessage sendSingleQuery() {
    return this.getMessenger().sendSingleChannelMessageQuery(this);
  }

  private CloudMessenger getMessenger() {
    return CloudNetDriver.getInstance().getMessenger();
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeObject(this.sender);
    buffer.writeString(this.channel);
    buffer.writeOptionalString(this.message);
    buffer.writeOptionalString(this.json != null ? this.json.toJson() : null);
    buffer.writeOptionalArray(this.buffer != null ? this.buffer.toArray() : null);
    buffer.writeObjectCollection(this.targets);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.sender = buffer.readObject(ChannelMessageSender.class);
    this.channel = buffer.readString();
    this.message = buffer.readOptionalString();
    String headerJson = buffer.readOptionalString();
    this.json = headerJson != null ? JsonDocument.newDocument(headerJson) : null;
    byte[] body = buffer.readOptionalArray();
    this.buffer = body != null ? ProtocolBuffer.wrap(body) : null;
    this.targets = buffer.readObjectCollection(ChannelMessageTarget.class);
  }

  public static class Builder {

    private final ChannelMessage channelMessage;

    private Builder(ChannelMessageSender sender) {
      this.channelMessage = new ChannelMessage(sender);
    }

    public Builder channel(@NotNull String channel) {
      this.channelMessage.channel = channel;
      return this;
    }

    public Builder message(@Nullable String message) {
      this.channelMessage.message = message;
      return this;
    }

    public Builder json(@NotNull JsonDocument document) {
      this.channelMessage.json = document;
      return this;
    }

    public Builder buffer(@NotNull byte[] bytes) {
      return this.buffer(bytes == null ? null : ProtocolBuffer.wrap(bytes));
    }

    public Builder buffer(@Nullable ProtocolBuffer buffer) {
      this.channelMessage.buffer = buffer == null ? ProtocolBuffer.EMPTY : buffer;
      return this;
    }

    public Builder target(@NotNull ChannelMessageTarget target) {
      this.channelMessage.targets.add(target);
      return this;
    }

    public Builder target(@NotNull ChannelMessageTarget.Type type, @Nullable String name) {
      return this.target(new ChannelMessageTarget(type, name));
    }

    public Builder target(@NotNull DriverEnvironment environment, @Nullable String name) {
      return this.target(
        environment == DriverEnvironment.CLOUDNET ? ChannelMessageTarget.Type.NODE : ChannelMessageTarget.Type.SERVICE,
        name);
    }

    public Builder targetAll(@NotNull ChannelMessageTarget.Type type) {
      return this.target(type, null);
    }

    public Builder targetAll() {
      return this.target(ChannelMessageTarget.Type.ALL, null);
    }

    public Builder targetServices() {
      return this.targetAll(ChannelMessageTarget.Type.SERVICE);
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

    public Builder targetNodes() {
      return this.targetAll(ChannelMessageTarget.Type.NODE);
    }

    public Builder targetEnvironment(@NotNull ServiceEnvironmentType environment) {
      return this.target(new ChannelMessageTarget(environment));
    }

    public ChannelMessage build() {
      Preconditions.checkNotNull(this.channelMessage.channel, "No channel provided");
      if (this.channelMessage.targets.isEmpty()) {
        this.targetAll();
      }
      return this.channelMessage;
    }

  }

}
