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

package eu.cloudnetservice.driver.network.chunk.network;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import eu.cloudnetservice.driver.network.chunk.data.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.NonNull;

/**
 * A listener for chunked packets, opening the chunked pocket sessions.
 *
 * @since 4.0
 */
public class ChunkedPacketListener implements PacketListener {

  private final Function<ChunkSessionInformation, ChunkedPacketHandler> handlerFactory;
  private final Map<ChunkSessionInformation, ChunkedPacketHandler> runningSessions = new ConcurrentHashMap<>();

  /**
   * Creates a new packet listener instance.
   *
   * @param handlerFactory the factory to create the chunked packet handlers when receiving the initial request.
   * @throws NullPointerException if the given factory is null.
   */
  public ChunkedPacketListener(@NonNull Function<ChunkSessionInformation, ChunkedPacketHandler> handlerFactory) {
    this.handlerFactory = handlerFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) throws Exception {
    // read the chunk information from the buffer
    var information = packet.content().readObject(ChunkSessionInformation.class);
    // read the chunk index
    var chunkIndex = packet.content().readInt();
    // get or create the session associated with the packet
    var handler = this.runningSessions.computeIfAbsent(information, this.handlerFactory);
    // post the packet and check if the session is done
    if (handler.handleChunkPart(chunkIndex, packet.content())) {
      // done, remove the session
      this.runningSessions.remove(information);
    }
  }
}
