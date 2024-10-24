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

package eu.cloudnetservice.driver.impl.network.chunk;

import eu.cloudnetservice.driver.impl.network.chunk.network.ChunkedPacket;
import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * Represents a default implementation of a chunked packet sender specifically created for chunked transferring of a
 * huge file, e.g. a zip archive.
 *
 * @since 4.0
 */
public class DefaultFileChunkPacketSender extends DefaultChunkedPacketProvider implements ChunkedPacketSender {

  protected final InputStream source;
  protected final Consumer<Packet> packetSplitter;

  /**
   * Constructs a new chunked packet sender for file transfer.
   *
   * @param sessionInformation the information about the chunked session.
   * @param source             the source stream of the file, will be closed automatically.
   * @param packetSplitter     the splitter for each chunk part to transfer.
   * @throws NullPointerException if either the information, source or splitter is null.
   */
  public DefaultFileChunkPacketSender(
    @NonNull ChunkSessionInformation sessionInformation,
    @NonNull InputStream source,
    @NonNull Consumer<Packet> packetSplitter
  ) {
    super(sessionInformation);

    this.source = source;
    this.packetSplitter = packetSplitter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<TransferStatus> transferChunkedData() {
    return TaskUtil.supplyAsync(() -> {
      var chunkIndex = 0;
      var backingArray = new byte[this.chunkSessionInformation.chunkSize()];

      while (true) {
        var bytesRead = Math.max(0, this.source.read(backingArray));
        if (bytesRead == backingArray.length) {
          // if the bytes read is the same size as the backing array, then a full chunk of data has been read from the
          // backing file. this usually indicates that the chunk is not the last chunk in the transfer
          this.chunkSessionInformation.transferInformation().acquire();
          var chunkPacket = ChunkedPacket.createFullChunk(chunkIndex++, backingArray, this.chunkSessionInformation);
          this.packetSplitter.accept(chunkPacket);
        } else {
          // final chunk to send out, this is one is allowed to not contain as much data as the other chunks
          var chunkPacket = ChunkedPacket.createFinalChunk(
            chunkIndex,
            bytesRead,
            backingArray,
            this.chunkSessionInformation);
          this.packetSplitter.accept(chunkPacket);

          // close all allocated resources used for the transfer
          this.source.close();
          this.chunkSessionInformation.transferInformation().release();

          return TransferStatus.SUCCESS;
        }
      }
    });
  }
}
