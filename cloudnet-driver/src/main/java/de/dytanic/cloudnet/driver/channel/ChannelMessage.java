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
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.netty.buffer.NettyImmutableDataBuf;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.serialization.DefaultProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public class ChannelMessage {

  public static final Type COLLECTION_TYPE = TypeToken.getParameterized(Collection.class, ChannelMessage.class)
    .getType();

  private String channel;
  private String message;

  private DataBuf content;
  private final ChannelMessageSender sender;

  private JsonDocument json = JsonDocument.EMPTY;
  private final Collection<ChannelMessageTarget> targets;

  protected ChannelMessage(@NotNull ChannelMessageSender sender) {
    this.sender = sender;
    this.targets = new ArrayList<>();
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

  /**
   * @deprecated Use {@link #getContent()} instead.
   */
  @Deprecated
  @ScheduledForRemoval
  public @NotNull ProtocolBuffer getBuffer() {
    return ProtocolBuffer.wrap(((NettyImmutableDataBuf) this.content).getByteBuf());
  }

  public @NotNull DataBuf getContent() {
    return this.content;
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

    /**
     * @deprecated Use the enhanced {@link #buffer(DataBuf)} instead.
     */
    @Deprecated
    @ScheduledForRemoval
    public Builder buffer(byte[] bytes) {
      return this.buffer(bytes == null ? null : ProtocolBuffer.wrap(bytes));
    }

    /**
     * @deprecated Use {@link #buffer(DataBuf)} instead.
     */
    @Deprecated
    @ScheduledForRemoval
    public Builder buffer(@Nullable ProtocolBuffer buffer) {
      this.channelMessage.content = buffer == null
        ? null
        : new NettyImmutableDataBuf(((DefaultProtocolBuffer) buffer).wrapped);
      return this;
    }

    public @NotNull Builder buffer(@Nullable DataBuf dataBuf) {
      this.channelMessage.content = dataBuf;
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
      return this.target(environment == DriverEnvironment.CLOUDNET
        ? ChannelMessageTarget.Type.NODE
        : ChannelMessageTarget.Type.SERVICE, name);
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
