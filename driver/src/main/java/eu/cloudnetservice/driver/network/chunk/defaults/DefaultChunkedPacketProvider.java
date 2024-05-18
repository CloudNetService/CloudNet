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

package eu.cloudnetservice.driver.network.chunk.defaults;

import eu.cloudnetservice.driver.network.chunk.ChunkedPacketProvider;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.chunk.data.ChunkSessionInformation;
import lombok.NonNull;

/**
 * Represents a default, shared implementation of a chunked packet provider.
 *
 * @since 4.0
 */
public abstract class DefaultChunkedPacketProvider implements ChunkedPacketProvider {

  protected final ChunkSessionInformation chunkSessionInformation;
  protected TransferStatus transferStatus;

  /**
   * Creates a new instance of this class.
   *
   * @param sessionInformation the information about the chunked session.
   * @throws NullPointerException if the given session information is null.
   */
  protected DefaultChunkedPacketProvider(@NonNull ChunkSessionInformation sessionInformation) {
    this.chunkSessionInformation = sessionInformation;
    this.transferStatus = TransferStatus.RUNNING;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransferStatus transferStatus() {
    return this.transferStatus;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkSessionInformation sessionInformation() {
    return this.chunkSessionInformation;
  }
}
