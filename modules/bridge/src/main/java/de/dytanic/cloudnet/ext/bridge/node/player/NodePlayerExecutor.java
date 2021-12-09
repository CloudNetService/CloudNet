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

package de.dytanic.cloudnet.ext.bridge.node.player;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.player.executor.ServerSelectorType;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodePlayerExecutor implements PlayerExecutor {

  protected static final PlayerExecutor GLOBAL = new NodePlayerExecutor(GLOBAL_UNIQUE_ID);

  protected final UUID targetUniqueId;
  protected final IPlayerManager playerManager;

  @Internal
  protected NodePlayerExecutor(@NotNull UUID targetUniqueId) {
    this.targetUniqueId = targetUniqueId;
    this.playerManager = null;
  }

  public NodePlayerExecutor(@NotNull UUID targetUniqueId, @NotNull IPlayerManager playerManager) {
    this.targetUniqueId = targetUniqueId;
    this.playerManager = playerManager;
  }

  @Override
  public @NotNull UUID getPlayerUniqueId() {
    return this.targetUniqueId;
  }

  @Override
  public void connect(@NotNull String serviceName) {
    this.toProxy()
      .message("connect_to_service")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId).writeString(serviceName))
      .build()
      .send();
  }

  @Override
  public void connectSelecting(@NotNull ServerSelectorType selectorType) {
    this.toProxy()
      .message("connect_to_selector")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId).writeObject(selectorType))
      .build()
      .send();
  }

  @Override
  public void connectToFallback() {
    this.toProxy()
      .message("connect_to_fallback")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId))
      .build()
      .send();
  }

  @Override
  public void connectToGroup(@NotNull String group, @NotNull ServerSelectorType selectorType) {
    this.toProxy()
      .message("connect_to_group")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId).writeString(group).writeObject(selectorType))
      .build()
      .send();
  }

  @Override
  public void connectToTask(@NotNull String task, @NotNull ServerSelectorType selectorType) {
    this.toProxy()
      .message("connect_to_task")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId).writeString(task).writeObject(selectorType))
      .build()
      .send();
  }

  @Override
  public void kick(@NotNull Component message) {
    this.toProxy()
      .message("kick_player")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId).writeObject(message))
      .build()
      .send();
  }

  @Override
  public void sendTitle(@NotNull Title title) {
    this.toProxy()
      .message("send_title")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId).writeObject(title))
      .build()
      .send();
  }

  @Override
  public void sendMessage(@NotNull Component message) {
    this.sendChatMessage(message, null);
  }

  @Override
  public void sendChatMessage(@NotNull Component message, @Nullable String permission) {
    this.toProxy()
      .message("send_chat_message")
      .buffer(DataBuf.empty()
        .writeUniqueId(this.targetUniqueId)
        .writeObject(message)
        .writeNullable(permission, DataBuf.Mutable::writeString))
      .build()
      .send();
  }

  @Override
  public void sendPluginMessage(@NotNull String tag, byte[] data) {
    this.toProxy()
      .message("send_plugin_message")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId).writeString(tag).writeByteArray(data))
      .build()
      .send();
  }

  @Override
  public void dispatchProxyCommand(@NotNull String command) {
    this.toProxy()
      .message("dispatch_proxy_command")
      .buffer(DataBuf.empty().writeUniqueId(this.targetUniqueId).writeString(command))
      .build()
      .send();
  }

  protected @NotNull ChannelMessage.Builder toProxy() {
    // select the correct builder
    ChannelMessage.Builder message;
    if (this.targetUniqueId.equals(GLOBAL_UNIQUE_ID)) {
      // target all proxies if this is the global executor
      message = ChannelMessage.builder()
        .targetEnvironment(ServiceEnvironmentType.VELOCITY)
        .targetEnvironment(ServiceEnvironmentType.BUNGEECORD)
        .targetEnvironment(ServiceEnvironmentType.WATERDOG_PE);
    } else {
      // get the player associated with this provider
      //noinspection ConstantConditions - This can never be null here (only for the global unique id which is handeled already)
      CloudPlayer player = this.playerManager.getOnlinePlayer(this.targetUniqueId);
      // the player must be connected to proceed
      Preconditions.checkNotNull(player, "Target player %s is not connected (anymore)", this.targetUniqueId);
      // target the login service of the player
      message = ChannelMessage.builder().targetService(player.getLoginService().getServerName());
    }
    // set the internal bridge channel
    return message.channel(BridgeManagement.BRIDGE_PLAYER_EXECUTOR_CHANNEL_NAME);
  }
}
