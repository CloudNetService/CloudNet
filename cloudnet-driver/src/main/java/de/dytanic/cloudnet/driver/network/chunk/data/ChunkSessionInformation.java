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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class ChunkSessionInformation {

  private final int chunkSize;
  private final int transferType;
  private final UUID sessionUniqueId;
  private final JsonDocument transferInformation;

  public ChunkSessionInformation(
    int chunkSize,
    int transferType,
    @NotNull UUID sessionUniqueId,
    @NotNull JsonDocument transferInformation
  ) {
    this.chunkSize = chunkSize;
    this.transferType = transferType;
    this.sessionUniqueId = sessionUniqueId;
    this.transferInformation = transferInformation;
  }

  public int getChunkSize() {
    return this.chunkSize;
  }

  public int getTransferType() {
    return this.transferType;
  }

  public @NotNull UUID getSessionUniqueId() {
    return this.sessionUniqueId;
  }

  public @NotNull JsonDocument getTransferInformation() {
    return this.transferInformation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof ChunkSessionInformation)) {
      return false;
    } else {
      ChunkSessionInformation that = (ChunkSessionInformation) o;
      return this.getSessionUniqueId().equals(that.getSessionUniqueId());
    }
  }

  @Override
  public int hashCode() {
    return this.getSessionUniqueId().hashCode();
  }
}
