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

package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.player.executor.DefaultPlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultPlayerManager implements IPlayerManager {

  private static final PlayerExecutor GLOBAL_PLAYER_EXECUTOR = new DefaultPlayerExecutor(
    DefaultPlayerExecutor.GLOBAL_ID);

  @Override
  public @NotNull PlayerExecutor getPlayerExecutor(@NotNull UUID uniqueId) {
    return new DefaultPlayerExecutor(uniqueId);
  }

  @Override
  public PlayerExecutor getGlobalPlayerExecutor() {
    return GLOBAL_PLAYER_EXECUTOR;
  }

  @Override
  public void broadcastMessage(@NotNull String message) {
    this.getGlobalPlayerExecutor().sendChatMessage(message);
  }

  @Override
  public void broadcastMessage(@NotNull String message, @Nullable String permission) {
    this.getGlobalPlayerExecutor().sendChatMessage(message, permission);
  }

  public ChannelMessage.Builder messageBuilder() {
    return ChannelMessage.builder().channel(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL);
  }

}
