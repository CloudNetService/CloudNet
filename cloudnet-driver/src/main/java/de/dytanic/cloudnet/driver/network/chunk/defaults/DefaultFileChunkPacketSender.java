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

package de.dytanic.cloudnet.driver.network.chunk.defaults;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketSender;
import de.dytanic.cloudnet.driver.network.chunk.TransferStatus;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import de.dytanic.cloudnet.driver.network.chunk.network.ChunkedPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import java.io.InputStream;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class DefaultFileChunkPacketSender extends DefaultChunkedPacketProvider implements ChunkedPacketSender {

  protected final InputStream source;
  protected final Consumer<IPacket> packetSplitter;

  public DefaultFileChunkPacketSender(
    @NotNull ChunkSessionInformation sessionInformation,
    @NotNull InputStream source,
    @NotNull Consumer<IPacket> packetSplitter
  ) {
    super(sessionInformation);

    this.source = source;
    this.packetSplitter = packetSplitter;
  }

  @Override
  public @NotNull InputStream getSource() {
    return this.source;
  }

  @Override
  public @NotNull Consumer<IPacket> getChunkPacketSplitter() {
    return this.packetSplitter;
  }

  @Override
  public @NotNull ITask<TransferStatus> transferChunkedData() {
    return CompletableTask.supplyAsync(() -> {
      int readCalls = 0;
      byte[] backingArray = new byte[this.chunkSessionInformation.getChunkSize()];

      while (true) {
        int bytesRead = this.source.read(backingArray);
        if (bytesRead == backingArray.length) {
          this.packetSplitter.accept(
            ChunkedPacket.createChunk(this.chunkSessionInformation, readCalls++, backingArray));
        } else {
          this.packetSplitter.accept(
            ChunkedPacket.createChunk(this.chunkSessionInformation, readCalls, readCalls, bytesRead, backingArray));
          return TransferStatus.SUCCESS;
        }
      }
    });
  }
}
