/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.chunk.defaults;

import eu.cloudnetservice.cloudnet.common.concurrent.CompletableTask;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.cloudnet.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import eu.cloudnetservice.cloudnet.driver.network.chunk.network.ChunkedPacket;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
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
    return CompletableTask.supply(() -> {
      var readCalls = 0;
      var backingArray = new byte[this.chunkSessionInformation.chunkSize()];

      while (true) {
        var bytesRead = this.source.read(backingArray);
        if (bytesRead != -1 && bytesRead == backingArray.length) {
          this.packetSplitter.accept(
            ChunkedPacket.createChunk(this.chunkSessionInformation, readCalls++, backingArray));
        } else {
          this.packetSplitter.accept(ChunkedPacket.createChunk(
            this.chunkSessionInformation,
            readCalls,
            readCalls,
            bytesRead == -1 ? 0 : bytesRead,
            bytesRead == -1 ? EMPTY_BYTE_ARRAY : backingArray));
          // close the stream after reading the final chunk
          this.source.close();
          // release the extra content now
          this.chunkSessionInformation.transferInformation().enableReleasing().release();
          // successful transfer
          return TransferStatus.SUCCESS;
        }
      }
    });
  }
}
