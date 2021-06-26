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

package de.dytanic.cloudnet.driver.network.protocol.chunk.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacket;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ChunkedPacketSession {

  private final INetworkChannel channel;
  private final ChunkedPacketListener listener;
  private final OutputStream outputStream;
  private final Collection<ChunkedPacket> pendingPackets = new ArrayList<>();
  private final Map<String, Object> properties;
  private final UUID sessionUniqueId;

  private ChunkedPacket firstPacket;
  private ChunkedPacket lastPacket;

  private int chunkId = 0;
  private JsonDocument header = JsonDocument.EMPTY;
  private volatile boolean closed;

  public ChunkedPacketSession(INetworkChannel channel, ChunkedPacketListener listener, UUID sessionUniqueId,
    OutputStream outputStream, Map<String, Object> properties) {
    this.channel = channel;
    this.listener = listener;
    this.sessionUniqueId = sessionUniqueId;
    this.outputStream = outputStream;
    this.properties = properties;
  }

  public void handleIncomingChunk(@NotNull ChunkedPacket packet) throws IOException {
    if (this.closed) {
      packet.clearData();
      throw new IllegalStateException(
        String.format("Session is already closed but received packet %d, %b", packet.getChunkId(), packet.isEnd()));
    }

    if (packet.getChunkId() == 0 && this.header.isEmpty() && !packet.getHeader().isEmpty()) {
      this.header = packet.getHeader();
      this.firstPacket = packet;
    }

    if (packet.isEnd()) {
      this.lastPacket = packet;
    }

    try {
      if (this.chunkId != packet.getChunkId()) {
        this.pendingPackets.add(packet);
      } else {
        this.storeChunk(packet);
      }
    } finally {
      this.checkPendingPackets();
    }
  }

  private void storeChunk(ChunkedPacket packet) throws IOException {
    if (this.closed) {
      return;
    }

    if (packet.getChunkId() == 0) { // Ignore first packet because it has no data we need
      ++this.chunkId;
      return;
    }

    if (packet.isEnd()) {
      this.close();
      return;
    }

    ++this.chunkId;

    try {
      packet.readData(this.outputStream);
    } finally {
      this.outputStream.flush();
      packet.clearData();
    }
  }

  private void checkPendingPackets() throws IOException {
    if (!this.pendingPackets.isEmpty()) {
      Iterator<ChunkedPacket> iterator = this.pendingPackets.iterator();
      while (iterator.hasNext()) {
        ChunkedPacket pending = iterator.next();
        if (this.chunkId == pending.getChunkId() || (pending.isEnd() && this.chunkId - 1 == pending.getChunks())) {
          iterator.remove();
          this.storeChunk(pending);
        }
      }
    }
  }

  protected void close() throws IOException {
    if (!this.pendingPackets.isEmpty()) {
      String packets = this.pendingPackets.stream().map(ChunkedPacket::getChunkId).map(String::valueOf)
        .collect(Collectors.joining(", "));
      throw new IllegalStateException(
        String.format("Closing with %d pending packets: %s", this.pendingPackets.size(), packets));
    }

    this.closed = true;
    this.outputStream.close();

    System.gc();

    this.listener.getSessions().remove(this.sessionUniqueId);
    this.listener.handleComplete(this);
  }

  public INetworkChannel getChannel() {
    return this.channel;
  }

  public OutputStream getOutputStream() {
    return this.outputStream;
  }

  public ChunkedPacket getFirstPacket() {
    return this.firstPacket;
  }

  public ChunkedPacket getLastPacket() {
    return this.lastPacket;
  }

  public JsonDocument getHeader() {
    return this.header;
  }

  public boolean isClosed() {
    return this.closed;
  }

  public Map<String, Object> getProperties() {
    return this.properties;
  }

  public int getCurrentChunkId() {
    return this.chunkId;
  }
}
