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

package de.dytanic.cloudnet.driver.network.chunk.data;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class ChunkSessionInformation {

  private final int chunkSize;
  private final UUID sessionUniqueId;
  private final String transferChannel;
  private final DataBuf transferInformation;

  public ChunkSessionInformation(
    int chunkSize,
    @NotNull UUID sessionUniqueId,
    @NotNull String transferChannel,
    @NotNull DataBuf transferInformation
  ) {
    this.chunkSize = chunkSize;
    this.transferChannel = transferChannel;
    this.sessionUniqueId = sessionUniqueId;
    this.transferInformation = transferInformation;
  }

  public int getChunkSize() {
    return this.chunkSize;
  }

  public @NotNull String getTransferChannel() {
    return this.transferChannel;
  }

  public @NotNull UUID getSessionUniqueId() {
    return this.sessionUniqueId;
  }

  public @NotNull DataBuf getTransferInformation() {
    return this.transferInformation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof ChunkSessionInformation that)) {
      return false;
    } else {
      return this.getSessionUniqueId().equals(that.getSessionUniqueId());
    }
  }

  @Override
  public int hashCode() {
    return this.getSessionUniqueId().hashCode();
  }
}
