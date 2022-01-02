/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.channel;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.DriverEnvironment;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.provider.CloudMessenger;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import java.util.ArrayList;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public record ChannelMessage(
  @NonNull String channel,
  @NonNull String message,
  @NonNull DataBuf content,
  @NonNull ChannelMessageSender sender,
  @NonNull Collection<ChannelMessageTarget> targets
) {

  @Contract(" -> new")
  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Contract("_ -> new")
  public static @NonNull Builder buildResponseFor(@NonNull ChannelMessage input) {
    return builder().channel("").message("").target(input.sender.type(), input.sender.name());
  }

  public void send() {
    this.messenger().sendChannelMessage(this);
  }

  public @NonNull Task<Collection<ChannelMessage>> sendQueryAsync() {
    return this.messenger().sendChannelMessageQueryAsync(this);
  }

  public @NonNull Task<ChannelMessage> sendSingleQueryAsync() {
    return this.messenger().sendSingleChannelMessageQueryAsync(this);
  }

  public @NonNull Collection<ChannelMessage> sendQuery() {
    return this.messenger().sendChannelMessageQuery(this);
  }

  public @Nullable ChannelMessage sendSingleQuery() {
    return this.messenger().sendSingleChannelMessageQuery(this);
  }

  private @NonNull CloudMessenger messenger() {
    return CloudNetDriver.instance().messenger();
  }

  public static final class Builder {

    private final Collection<ChannelMessageTarget> targets = new ArrayList<>();

    private String channel;
    private String message;

    private DataBuf content;
    private ChannelMessageSender sender;

    public @NonNull Builder sender(@NonNull ChannelMessageSender sender) {
      this.sender = sender;
      return this;
    }

    public @NonNull Builder channel(@NonNull String channel) {
      this.channel = channel;
      return this;
    }

    public @NonNull Builder message(@NonNull String message) {
      this.message = message;
      return this;
    }

    public @NonNull Builder buffer(@Nullable DataBuf dataBuf) {
      this.content = dataBuf;
      return this;
    }

    public @NonNull Builder target(@NonNull ChannelMessageTarget target) {
      this.targets.add(target);
      return this;
    }

    public @NonNull Builder target(@NonNull ChannelMessageTarget.Type type, @Nullable String name) {
      return this.target(ChannelMessageTarget.of(type, name));
    }

    public @NonNull Builder target(@NonNull DriverEnvironment environment, @Nullable String name) {
      return this.target(environment == DriverEnvironment.CLOUDNET
        ? ChannelMessageTarget.Type.NODE
        : ChannelMessageTarget.Type.SERVICE, name);
    }

    public @NonNull Builder targetAll(@NonNull ChannelMessageTarget.Type type) {
      return this.target(type, null);
    }

    public @NonNull Builder targetAll() {
      return this.target(ChannelMessageTarget.Type.ALL, null);
    }

    public @NonNull Builder targetServices() {
      return this.targetAll(ChannelMessageTarget.Type.SERVICE);
    }

    public @NonNull Builder targetService(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.SERVICE, name);
    }

    public @NonNull Builder targetTask(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.TASK, name);
    }

    public @NonNull Builder targetNode(@Nullable String name) {
      return this.target(ChannelMessageTarget.Type.NODE, name);
    }

    public @NonNull Builder targetNodes() {
      return this.targetAll(ChannelMessageTarget.Type.NODE);
    }

    public @NonNull Builder targetEnvironment(@NonNull ServiceEnvironmentType environment) {
      return this.target(ChannelMessageTarget.environment(environment));
    }

    @Contract(" -> new")
    public @NonNull ChannelMessage build() {
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
