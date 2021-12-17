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

package de.dytanic.cloudnet.driver.network.chunk.network;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketHandler;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class ChunkedPacketListener implements IPacketListener {

  private final Function<ChunkSessionInformation, ChunkedPacketHandler> handlerFactory;
  private final Map<ChunkSessionInformation, ChunkedPacketHandler> runningSessions = new ConcurrentHashMap<>();

  public ChunkedPacketListener(@NotNull Function<ChunkSessionInformation, ChunkedPacketHandler> handlerFactory) {
    this.handlerFactory = handlerFactory;
  }

  @Override
  public void handle(@NotNull INetworkChannel channel, @NotNull IPacket packet) throws Exception {
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
