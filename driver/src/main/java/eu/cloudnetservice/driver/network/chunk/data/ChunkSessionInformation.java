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

package eu.cloudnetservice.driver.network.chunk.data;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.util.UUID;
import lombok.NonNull;

/**
 * Contains all needed information for a chunked data transfer to be initialized. The transfer information in this
 * object are there to allow writes of additional information needed for the transfer to work, for example the target
 * file name.
 *
 * @param chunkSize           the size of data transferred in each chunk, should always be the exact amount of bytes.
 * @param sessionUniqueId     the unique id of the transfer session, for identification reasons.
 * @param transferChannel     the name of the channel the data is transferred in, for identification reasons.
 * @param transferInformation additional information for the transfer handler to handle the chunks correctly.
 * @since 4.0
 */
public record ChunkSessionInformation(
  int chunkSize,
  @NonNull UUID sessionUniqueId,
  @NonNull String transferChannel,
  @NonNull DataBuf transferInformation
) {

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof ChunkSessionInformation that)) {
      return false;
    } else {
      return this.sessionUniqueId().equals(that.sessionUniqueId());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.sessionUniqueId().hashCode();
  }
}
