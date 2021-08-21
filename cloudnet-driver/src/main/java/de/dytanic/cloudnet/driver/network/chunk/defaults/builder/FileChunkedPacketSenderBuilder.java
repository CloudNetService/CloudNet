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

package de.dytanic.cloudnet.driver.network.chunk.defaults.builder;

import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketSender;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import de.dytanic.cloudnet.driver.network.chunk.defaults.DefaultFileChunkPacketSender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class FileChunkedPacketSenderBuilder extends DefaultChunkedPacketSenderBuilder {

  public @NotNull FileChunkedPacketSenderBuilder forFile(@NotNull Path path) {
    try {
      this.source(Files.newInputStream(path));
      return this;
    } catch (IOException exception) {
      throw new AssertionError("Unexpected exception opening file stream", exception);
    }
  }

  @Override
  protected @NotNull ChunkedPacketSender doBuild() {
    return new DefaultFileChunkPacketSender(new ChunkSessionInformation(
      this.chunkSize,
      this.transferType,
      this.sessionUniqueId,
      this.transferInformation
    ), this.source, this.packetSplitter);
  }
}
