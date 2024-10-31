/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.network;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.network.ChannelType;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelInitEvent;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.impl.network.chunk.ChunkedSessionRegistry;
import eu.cloudnetservice.driver.impl.network.chunk.network.ChunkedPacketListener;
import eu.cloudnetservice.driver.impl.network.rpc.listener.RPCPacketListener;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.chunk.event.EventChunkHandlerFactory;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.node.impl.network.listener.ChannelMessagePacketListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class NodeNetworkUtil {

  private final EventManager eventManager;
  private final ChunkedSessionRegistry chunkedSessionRegistry;

  @Inject
  public NodeNetworkUtil(@NonNull EventManager eventManager, @NonNull ChunkedSessionRegistry chunkedSessionRegistry) {
    this.eventManager = eventManager;
    this.chunkedSessionRegistry = chunkedSessionRegistry;
  }

  boolean shouldInitializeChannel(@NonNull NetworkChannel channel, @NonNull ChannelType type) {
    return !this.eventManager.callEvent(new NetworkChannelInitEvent(channel, type)).cancelled();
  }

  public void addDefaultPacketListeners(@NonNull PacketListenerRegistry registry) {
    registry.addListener(NetworkConstants.CHANNEL_MESSAGING_CHANNEL, ChannelMessagePacketListener.class);
    registry.addListener(NetworkConstants.INTERNAL_RPC_COM_CHANNEL, RPCPacketListener.class);

    var chunkedListener = new ChunkedPacketListener(
      this.chunkedSessionRegistry,
      new EventChunkHandlerFactory(this.eventManager));
    registry.addListener(NetworkConstants.CHUNKED_PACKET_COM_CHANNEL, chunkedListener);
  }
}
