/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.chunk.defaults;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.chunk.data.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.network.ChunkedPacket;
import eu.cloudnetservice.driver.network.protocol.Packet;
import java.io.InputStream;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * Represents a default implementation of a chunked packet sender specifically created for chunked transferring of a
 * huge file, e.g. a zip archive.
 * <p>
 * This class shouldn't get instantiated directly, use {@link ChunkedPacketSender#forFileTransfer()} instead.
 *
 * @since 4.0
 */
public class DefaultFileChunkPacketSender extends DefaultChunkedPacketProvider implements ChunkedPacketSender {

  protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

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
  public @NonNull InputStream source() {
    return this.source;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Consumer<Packet> packetSplitter() {
    return this.packetSplitter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<TransferStatus> transferChunkedData() {
    return Task.supply(() -> {
      var readCalls = 0;
      var backingArray = new byte[this.chunkSessionInformation.chunkSize()];

      while (true) {
        var bytesRead = this.source.read(backingArray);
        if (bytesRead != -1 && bytesRead == backingArray.length) {
          // acquire the transfer information once before writing the data of the chunk
          this.chunkSessionInformation.transferInformation().acquire();
          this.packetSplitter.accept(ChunkedPacket.createChunk(
            this.chunkSessionInformation,
            readCalls++,
            backingArray));
        } else {
          this.packetSplitter.accept(ChunkedPacket.createChunk(
            this.chunkSessionInformation,
            readCalls,
            readCalls,
            bytesRead == -1 ? 0 : bytesRead,
            bytesRead == -1 ? EMPTY_BYTE_ARRAY : backingArray));

          // close the stream after reading the final chunk & release the extra content now
          this.source.close();
          this.chunkSessionInformation.transferInformation().release();

          // successful transfer
          return TransferStatus.SUCCESS;
        }
      }
    });
  }
}
