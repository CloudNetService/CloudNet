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

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import java.util.ArrayList;
import java.util.Collection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ChannelMessage(
  @NotNull String channel,
  @NotNull String message,
  @NotNull DataBuf content,
  @NotNull ChannelMessageSender sender,
  @NotNull Collection<ChannelMessageTarget> targets
) {

  @Contract(" -> new")
  public static @NotNull Builder builder() {
    return new Builder();
  }

  @Contract("_ -> new")
  public static @NotNull Builder buildResponseFor(@NotNull ChannelMessage input) {
    return builder().channel("").target(input.sender.type(), input.sender.name());
  }

  public void send() {
    this.messenger().sendChannelMessage(this);
  }

  public @NotNull ITask<Collection<ChannelMessage>> sendQueryAsync() {
    return this.messenger().sendChannelMessageQueryAsync(this);
  }

  public @NotNull ITask<ChannelMessage> sendSingleQueryAsync() {
    return this.messenger().sendSingleChannelMessageQueryAsync(this);
  }

  public @NotNull Collection<ChannelMessage> sendQuery() {
    return this.messenger().sendChannelMessageQuery(this);
  }

  public @Nullable ChannelMessage sendSingleQuery() {
    return this.messenger().sendSingleChannelMessageQuery(this);
  }

  private @NotNull CloudMessenger messenger() {
    return CloudNetDriver.getInstance().getMessenger();
  }

  public static final class Builder {

    private final Collection<ChannelMessageTarget> targets = new ArrayList<>();

    private String channel;
    private String message;

    private DataBuf content;
    private ChannelMessageSender sender;

    public @NotNull Builder sender(@NotNull ChannelMessageSender sender) {
      this.sender = sender;
      return this;
    }

    public @NotNull Builder channel(@NotNull String channel) {
      this.channel = channel;
      return this;
    }

    public @NotNull Builder message(@NotNull String message) {
      this.message = message;
      return this;
    }

    public @NotNull Builder buffer(@Nullable DataBuf dataBuf) {
      this.content = dataBuf;
      return this;
    }

    public @NotNull Builder target(@NotNull ChannelMessageTarget target) {
      this.targets.add(target);
      return this;
    }

    public @NotNull Builder target(@NotNull ChannelMessageTarget.Type type, @Nullable String name) {
      return this.target(ChannelMessageTarget.of(type, name));
    }

    public @NotNull Builder target(@NotNull DriverEnvironment environment, @Nullable String name) {
      return this.target(environment == DriverEnvironment.CLOUDNET
        ? ChannelMessageTarget.Type.NODE
        : ChannelMessageTarget.Type.SERVICE, name);
    }

    public @NotNull Builder targetAll(@NotNull ChannelMessageTarget.Type type) {
      return this.target(type, null);
    }

    public @NotNull Builder targetAll() {
      return this.target(ChannelMessageTarget.Type.ALL, null);
    }

    public @NotNull Builder targetServices() {
      return this.targetAll(ChannelMessageTarget.Type.SERVICE);
    }

    public @NotNull Builder targetService(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.SERVICE, name);
    }

    public @NotNull Builder targetTask(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.TASK, name);
    }

    public @NotNull Builder targetNode(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.NODE, name);
    }

    public @NotNull Builder targetNodes() {
      return this.targetAll(ChannelMessageTarget.Type.NODE);
    }

    public @NotNull Builder targetEnvironment(@NotNull ServiceEnvironmentType environment) {
      return this.target(ChannelMessageTarget.environment(environment));
    }

    @Contract(" -> new")
    public @NotNull ChannelMessage build() {
      Verify.verifyNotNull(this.channel, "No channel provided");
      Verify.verifyNotNull(this.message, "No message provided");
      Verify.verify(!this.targets.isEmpty(), "No targets provided");

      return new ChannelMessage(
        this.channel,
        this.message,
        this.content == null ? DataBuf.empty() : this.content,
        this.sender == null ? ChannelMessageSender.self() : this.sender,
        this.targets);
    }
  }
}
