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

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;

/**
 * A handler for chunked packets parts. A handler is normally created through some kind of factory which decides which
 * handler should be used based on the supplied information by the network component trying to send the data into the
 * cluster. By default, the ChunkedPacketSessionOpenEvent is called and the handler set in the event will be used to
 * handle the full chunked data written by the other component.
 * <p>
 * A handler is not required to write the whole data the other component is sending. However, it is a good advise to not
 * store the full data in the memory, as this is not the reason why the data is sent chunked to the current network
 * component.
 *
 * @since 4.0
 */
public interface ChunkedPacketHandler extends ChunkedPacketProvider {

  /**
   * Handles a part of chunked data which gets supplied to the current component. The data chunk is required to be from
   * the same size as announced in the initial transfer information, unless the packet is the last packet of the chunked
   * data stream.
   *
   * @param chunkPosition the position of the chunk, starting from 0.
   * @param dataBuf       the data in the chunk.
   * @return true, if the chunk was the last chunk and the callback was called, false otherwise or in case of a failure.
   * @throws NullPointerException if the given data buf is null.
   */
  boolean handleChunkPart(int chunkPosition, @NonNull DataBuf dataBuf);

  /**
   * A callback called once the full data of the chunk session was received successfully.
   *
   * @since 4.0
   */
  @FunctionalInterface
  interface Callback {

    /**
     * Handles the completion and therefore full receive of the data transferred by the other component.
     *
     * @param information the information about the chunk session which were sent initially.
     * @param dataInput   the stream of data sent to this component in this chunked session.
     * @return true if the full data was consumed and the given stream can be closed, false otherwise.
     * @throws IOException          in case an i/o exception happens during result handling.
     * @throws NullPointerException if the given session information or data input stream is null.
     */
    boolean handleSessionComplete(
      @NonNull ChunkSessionInformation information,
      @NonNull InputStream dataInput) throws IOException;
  }
}
