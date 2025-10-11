/*
 * Copyright 2019-present CloudNetService team & contributors
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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

  protected static final int CHUNK_INFO_DATA_BYTES = Byte.BYTES + Integer.BYTES; // eof (Z) + byte count (I)

  protected final ReadableByteChannel source;
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
    this.packetSplitter = packetSplitter;
    this.source = source instanceof FileInputStream fis ? fis.getChannel() : Channels.newChannel(source);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<TransferStatus> transferChunkedData() {
    return TaskUtil.supplyAsync(() -> {
      var chunkIndex = 0;
      var chunkSize = this.chunkSessionInformation.chunkSize();
      var bufferGrowthPerChunk = CHUNK_INFO_DATA_BYTES + chunkSize; // chunk info + actual chunk data
      while (true) {
        var chunkDataBuffer = ChunkedPacket
          .createBaseChunkDataBuffer(chunkIndex++, this.chunkSessionInformation)
          .ensureWriteable(bufferGrowthPerChunk);
        var writerOffset = chunkDataBuffer.writerOffset();

        // advance the writer offset to skip the bytes for the EOF and chunk size info. these
        // are only available after writing the data, so that has to be done first
        chunkDataBuffer.advanceWriterOffset(CHUNK_INFO_DATA_BYTES);
        var chunkDataNioBuffer = chunkDataBuffer
          .writeableNioBuffer()
          .limit(chunkDataBuffer.writerOffset() + chunkSize);
        var bytesRead = Math.max(0, this.source.read(chunkDataNioBuffer));

        // move the writer offset back to write the EOF and chunk size info,
        // then move the writer offset back to the end of the buffer
        chunkDataBuffer.writerOffset(writerOffset);
        var finalChunk = bytesRead != chunkSize;
        chunkDataBuffer.writeBoolean(finalChunk).writeInt(bytesRead);
        chunkDataBuffer.advanceWriterOffset(bytesRead);

        var chunkPacket = new ChunkedPacket(chunkDataBuffer);
        this.packetSplitter.accept(chunkPacket);

        if (finalChunk) {
          this.source.close();
          this.chunkSessionInformation.close();
          return TransferStatus.SUCCESS;
        }
      }
    });
  }
}
