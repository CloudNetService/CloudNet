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

package eu.cloudnetservice.driver.network.chunk.network;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.data.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A chunked packet which gets transferred from the sender to the target each time holding necessary information for
 * packet processing on the other side.
 *
 * @since 4.0
 */
public class ChunkedPacket extends BasePacket {

  /**
   * Creates a new chunk part. The given buffer must contain all needed information for the receiver.
   * <p>
   * Using this constructor is not recommended, better use {@link #createChunk(ChunkSessionInformation, int, byte[])} or
   * {@link #createChunk(ChunkSessionInformation, Integer, int, int, byte[])}, based on the state of the transfer.
   *
   * @param dataBuf the backing buffer.
   * @throws NullPointerException if the given buffer is null.
   */
  public ChunkedPacket(@NonNull DataBuf dataBuf) {
    super(NetworkConstants.CHUNKED_PACKET_COM_CHANNEL, dataBuf);
  }

  /**
   * Creates a new chunk part. This method is used when the full chunk data is not yet known. It gives no information
   * about the amount of chunks in the full transfer and uses the length of the given array as the data length.
   * <p>
   * This call is equivalent to {@code ChunkedPacket.createChunk(information, null, chunkIndex, data.length, data)}.
   *
   * @param information the session information this chunk belongs to.
   * @param chunkIndex  the index of the written chunk.
   * @param data        the data of the chunk.
   * @return the created chunk packet based on the information.
   * @throws NullPointerException if the given chunk information is null.
   */
  public static @NonNull ChunkedPacket createChunk(
    @NonNull ChunkSessionInformation information,
    int chunkIndex,
    byte[] data
  ) {
    return createChunk(information, null, chunkIndex, data.length, data);
  }

  /**
   * Creates a new chunk part based on the given information. This method is used primarily to create a chunk part when
   * all information needed to complete the transfer are available (normally when the last chunk was read from the
   * backing stream)
   *
   * @param information the session information this chunk belongs to.
   * @param chunkAmount the amount of chunks which were read from the backing buffer.
   * @param chunkIndex  the index of the written chunk.
   * @param dataLength  the amount of bytes in the current packet chunk.
   * @param data        the data of the chunk.
   * @return the created chunk packet based on the information.
   * @throws NullPointerException if the given chunk information is null.
   */
  public static @NonNull ChunkedPacket createChunk(
    @NonNull ChunkSessionInformation information,
    @Nullable Integer chunkAmount,
    int chunkIndex,
    int dataLength,
    byte[] data
  ) {
    var dataBuf = DataBuf.empty()
      // transfer information
      .writeObject(information)
      // the index of the chunk we are sending
      .writeInt(chunkIndex)
      // if the packet is the ending packet holding the information about the chunk amount
      .writeBoolean(chunkAmount != null);
    // if we know the chunk amount => write it
    if (chunkAmount != null) {
      dataBuf.writeInt(chunkAmount);
    }
    // write the actual content of the chunk
    return new ChunkedPacket(dataBuf.writeByteArray(data, dataLength));
  }
}
