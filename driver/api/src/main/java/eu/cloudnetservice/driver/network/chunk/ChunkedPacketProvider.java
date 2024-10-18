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

package eu.cloudnetservice.driver.network.chunk;

import lombok.NonNull;

/**
 * Represents an object which is associated to chunked packets in any way and provides general information.
 *
 * @since 4.0
 */
public interface ChunkedPacketProvider {

  /**
   * Get the current transfer status, either while sending or receiving.
   *
   * @return the current transfer status
   */
  @NonNull
  TransferStatus transferStatus();

  /**
   * Get the session information which is associated with this provider.
   *
   * @return the session information.
   */
  @NonNull
  ChunkSessionInformation sessionInformation();
}
