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

package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import org.jetbrains.annotations.NotNull;

public interface IClusterNodeServer extends NodeServer, AutoCloseable {

  void sendCustomChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

  void sendCustomChannelMessage(@NotNull ChannelMessage channelMessage);

  @Override
  @NotNull IClusterNodeServerProvider getProvider();

  INetworkChannel getChannel();

  void setChannel(@NotNull INetworkChannel channel);

  boolean isConnected();

  void saveSendPacket(@NotNull IPacket packet);

  void saveSendPacketSync(@NotNull IPacket packet);

  boolean isAcceptableConnection(@NotNull INetworkChannel channel, @NotNull String nodeId);

  @Override
  default boolean isAvailable() {
    return this.getChannel() != null;
  }
}
