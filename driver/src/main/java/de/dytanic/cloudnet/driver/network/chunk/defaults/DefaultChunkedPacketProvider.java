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

import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketProvider;
import de.dytanic.cloudnet.driver.network.chunk.TransferStatus;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import org.jetbrains.annotations.NotNull;

public abstract class DefaultChunkedPacketProvider implements ChunkedPacketProvider {

  protected final ChunkSessionInformation chunkSessionInformation;
  protected TransferStatus transferStatus;

  protected DefaultChunkedPacketProvider(ChunkSessionInformation sessionInformation) {
    this.chunkSessionInformation = sessionInformation;
    this.transferStatus = TransferStatus.RUNNING;
  }

  @Override
  public @NotNull TransferStatus transferStatus() {
    return this.transferStatus;
  }

  @Override
  public @NotNull ChunkSessionInformation sessionInformation() {
    return this.chunkSessionInformation;
  }
}
