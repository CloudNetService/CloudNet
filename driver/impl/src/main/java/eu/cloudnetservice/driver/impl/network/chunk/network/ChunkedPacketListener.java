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

package eu.cloudnetservice.driver.impl.network.chunk.network;

import eu.cloudnetservice.driver.impl.network.chunk.ChunkedSessionRegistry;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import java.util.function.Function;
import lombok.NonNull;

/**
 * A listener for chunked packets, opening the chunked pocket sessions.
 *
 * @since 4.0
 */
public final class ChunkedPacketListener implements PacketListener {

  private final ChunkedSessionRegistry sessionRegistry;
  private final Function<ChunkSessionInformation, ChunkedPacketHandler> handlerFactory;

  /**
   * Creates a new packet listener instance.
   *
   * @param handlerFactory the factory to create the chunked packet handlers when receiving the initial request.
   * @throws NullPointerException if the given factory is null.
   */
  public ChunkedPacketListener(
    @NonNull ChunkedSessionRegistry sessionRegistry,
    @NonNull Function<ChunkSessionInformation, ChunkedPacketHandler> handlerFactory
  ) {
    this.sessionRegistry = sessionRegistry;
    this.handlerFactory = handlerFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) throws Exception {
    var packetContent = packet.content();
    var sessionInfo = packetContent.readObject(ChunkSessionInformation.class);
    var chunkIndex = packetContent.readInt();

    // get or create a new local session for the transfer
    var sessionHandler = this.sessionRegistry.getOrCreateSession(sessionInfo, this.handlerFactory);
    var transferComplete = sessionHandler.handleChunkPart(chunkIndex, packetContent);
    if (transferComplete) {
      this.sessionRegistry.completeSession(sessionInfo.sessionUniqueId());
    }
  }
}
