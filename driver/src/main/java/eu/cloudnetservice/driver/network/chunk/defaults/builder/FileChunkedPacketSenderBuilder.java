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

package eu.cloudnetservice.driver.network.chunk.defaults.builder;

import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.data.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.defaults.DefaultFileChunkPacketSender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * Represents a builder for chunked packet sessions which are transferring files.
 *
 * @see DefaultFileChunkPacketSender
 * @see ChunkedPacketSender#forFileTransfer()
 * @since 4.0
 */
public class FileChunkedPacketSenderBuilder extends DefaultChunkedPacketSenderBuilder {

  /**
   * Sets the file to transfer in the session. The file path must exist.
   *
   * @param path the path to the file to transfer.
   * @return the same builder instance as used to call the method, for chaining.
   * @throws AssertionError       if an i/o error occurs while opening the file stream.
   * @throws NullPointerException if the given file path is null.
   */
  public @NonNull FileChunkedPacketSenderBuilder forFile(@NonNull Path path) {
    try {
      this.source(Files.newInputStream(path));
      return this;
    } catch (IOException exception) {
      throw new AssertionError("Unexpected exception opening file stream", exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull ChunkedPacketSender doBuild() {
    return new DefaultFileChunkPacketSender(new ChunkSessionInformation(
      this.chunkSize,
      this.sessionUniqueId,
      this.transferChannel,
      this.transferInformation
    ), this.source, this.packetSplitter);
  }
}
