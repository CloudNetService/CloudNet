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

package de.dytanic.cloudnet.ext.bridge.player.executor;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import java.util.Arrays;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultPlayerExecutor implements PlayerExecutor {

  public static final UUID GLOBAL_ID = new UUID(0, 0);
  private static final ServiceEnvironmentType[] TARGET_ENVIRONMENTS = Arrays.stream(ServiceEnvironmentType.values())
    .filter(ServiceEnvironmentType::isMinecraftProxy)
    .toArray(ServiceEnvironmentType[]::new);

  private final UUID uniqueId;

  public DefaultPlayerExecutor(@NotNull UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

  public static ChannelMessage.Builder builder() {
    ChannelMessage.Builder builder = ChannelMessage.builder()
      .channel(BridgeConstants.BRIDGE_PLAYER_API_CHANNEL);
    for (ServiceEnvironmentType targetEnvironment : TARGET_ENVIRONMENTS) {
      builder.targetEnvironment(targetEnvironment);
    }
    return builder;
  }

  @Override
  public @NotNull UUID getPlayerUniqueId() {
    return this.uniqueId;
  }

  @Override
  public void connect(@NotNull String serviceName) {
    Preconditions.checkNotNull(serviceName);

    builder()
      .message("connect_server")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId).writeString(serviceName))
      .build().send();
  }

  @Override
  public void kick(@NotNull String message) {
    Preconditions.checkNotNull(message);

    builder()
      .message("kick")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId).writeString(message))
      .build().send();
  }

  @Override
  public void sendChatMessage(@NotNull String message) {
    this.sendChatMessage(message, null);
  }

  @Override
  public void sendChatMessage(@NotNull String message, @Nullable String permission) {
    Preconditions.checkNotNull(message);

    builder()
      .message("send_message")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId).writeString(message).writeOptionalString(permission))
      .build().send();
  }

  @Override
  public void sendPluginMessage(@NotNull String tag, @NotNull byte[] data) {
    Preconditions.checkNotNull(tag);
    Preconditions.checkNotNull(data);

    builder()
      .message("send_plugin_message")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId).writeString(tag).writeArray(data))
      .build().send();
  }

  @Override
  public void dispatchProxyCommand(@NotNull String command) {
    Preconditions.checkNotNull(command);

    builder()
      .message("dispatch_proxy_command")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId).writeString(command))
      .build().send();
  }

  @Override
  public void connect(@NotNull ServerSelectorType selectorType) {
    builder()
      .message("connect_type")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId).writeEnumConstant(selectorType))
      .build().send();
  }

  @Override
  public void connectToFallback() {
    builder()
      .message("connect_fallback")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId))
      .build().send();
  }

  @Override
  public void connectToGroup(@NotNull String group, @NotNull ServerSelectorType selectorType) {
    builder()
      .message("connect_group")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId).writeString(group).writeEnumConstant(selectorType))
      .build().send();
  }

  @Override
  public void connectToTask(@NotNull String task, @NotNull ServerSelectorType selectorType) {
    builder()
      .message("connect_task")
      .buffer(ProtocolBuffer.create().writeUUID(this.uniqueId).writeString(task).writeEnumConstant(selectorType))
      .build().send();
  }

}
