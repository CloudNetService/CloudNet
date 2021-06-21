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

package de.dytanic.cloudnet.driver.network.protocol.chunk;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

public class ChunkedPacketBuilder {

  public static final int DEFAULT_CHUNK_SIZE = 128 * 1024;

  private InputStream inputStream;
  private Integer channel;
  private Consumer<ChunkedPacket> target;

  private UUID uniqueId = UUID.randomUUID();
  private JsonDocument header = JsonDocument.EMPTY;
  private int chunkSize = DEFAULT_CHUNK_SIZE;

  private boolean completed;
  private boolean success;

  public static ChunkedPacketBuilder newBuilder(int channel, InputStream inputStream) {
    return newBuilder().channel(channel).input(inputStream);
  }

  public static ChunkedPacketBuilder newBuilder() {
    return new ChunkedPacketBuilder();
  }

  public ChunkedPacketBuilder input(InputStream inputStream) {
    this.inputStream = inputStream;
    return this;
  }

  public ChunkedPacketBuilder input(Path path) throws IOException {
    return this.input(Files.newInputStream(path));
  }

  public ChunkedPacketBuilder channel(int channel) {
    this.channel = channel;
    return this;
  }

  public int channel() {
    return this.chunkSize;
  }

  public ChunkedPacketBuilder target(Consumer<ChunkedPacket> target) {
    this.target = target;
    return this;
  }

  public ChunkedPacketBuilder target(INetworkChannel channel) {
    return this.target(Collections.singletonList(channel));
  }

  public ChunkedPacketBuilder target(Collection<INetworkChannel> channels) {
    return this.target(DefaultChunkedPacketHandler.createHandler(channels));
  }

  public ChunkedPacketBuilder uniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId;
    return this;
  }

  public UUID uniqueId() {
    return this.uniqueId;
  }

  public ChunkedPacketBuilder header(JsonDocument header) {
    this.header = header;
    return this;
  }

  public JsonDocument header() {
    return this.header;
  }

  public ChunkedPacketBuilder chunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  public int chunkSize() {
    return this.chunkSize;
  }

  private ChunkedPacket createStartPacket(int channel, UUID uniqueId, JsonDocument header, int chunkSize) {
    return new ChunkedPacket(channel, uniqueId, header, 0, chunkSize, chunkSize, false, new byte[0], 0);
  }

  private ChunkedPacket createSegment(int channel, UUID uniqueId, int id, int chunkSize, int length, byte[] data) {
    return new ChunkedPacket(channel, uniqueId, JsonDocument.EMPTY, id, chunkSize, length, false, data, 0);
  }

  private ChunkedPacket createEndPacket(int channel, UUID uniqueId, int id, int chunkSize) {
    return new ChunkedPacket(channel, uniqueId, JsonDocument.EMPTY, id, chunkSize, 0, true, new byte[0], id - 1);
  }

  public ChunkedPacketBuilder complete() throws IOException {
    this.validate();

    try {
      this.target.accept(this.createStartPacket(this.channel, this.uniqueId, this.header, this.chunkSize));

      int chunkId = 1;

      int read;
      byte[] buffer = new byte[this.chunkSize];
      while ((read = this.inputStream.read(buffer)) != -1) {
        this.target
          .accept(this.createSegment(this.channel, this.uniqueId, chunkId++,
              this.chunkSize, read, Arrays.copyOf(buffer, buffer.length)));
      }

      this.target.accept(this.createEndPacket(this.channel, this.uniqueId, chunkId, this.chunkSize));
      this.inputStream.close();

      this.success = true;
    } catch (ChunkInterrupt ignored) {
    }

    this.completed = true;
    return this;
  }

  private void validate() {
    Preconditions.checkNotNull(this.inputStream, "No input provided");
    Preconditions.checkNotNull(this.target, "No handler provided");
    Preconditions.checkNotNull(this.channel, "No channel provided");
    Preconditions.checkState(!this.completed, "Builder cannot be completed twice");
  }

  public boolean isCompleted() {
    return this.completed;
  }

  public boolean isSuccess() {
    return this.success;
  }

}
