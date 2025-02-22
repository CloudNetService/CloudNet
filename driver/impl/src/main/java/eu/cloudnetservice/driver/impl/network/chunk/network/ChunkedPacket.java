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

package eu.cloudnetservice.driver.impl.network.chunk.network;

import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.impl.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import lombok.NonNull;

/**
 * A transfer packet that holds data about a chunk of data to transfer.
 *
 * @since 4.0
 */
public final class ChunkedPacket extends BasePacket {

  /**
   * Constructs a new chunked transfer part packet.
   *
   * @param dataBuf the data for the handling process of the chunk part.
   * @throws NullPointerException if the given buffer is null.
   */
  private ChunkedPacket(@NonNull DataBuf dataBuf) {
    super(NetworkConstants.CHUNKED_PACKET_COM_CHANNEL, dataBuf);
  }

  /**
   * Creates a new full chunk in the middle of a transfer. This method assumes that tbe amount of bytes that were read
   * from the underlying source is the same as the chunk size of the transfer, only the last chunk is allowed to contain
   * less or no data.
   *
   * @param chunkIndex  the 0-based index of the chunk that is being sent.
   * @param sourceData  the data that was read from the underlying source.
   * @param sessionInfo the information about the transfer session that this packet is related to.
   * @return a full chunk packet containing all the information provided to this method.
   * @throws NullPointerException if the given source data or chunk information is null.
   */
  public static @NonNull ChunkedPacket createFullChunk(
    int chunkIndex,
    byte[] sourceData,
    @NonNull ChunkSessionInformation sessionInfo
  ) {
    var sourceDataLengthSize = NettyUtil.varIntBytes(sourceData.length);
    var transferBytes = Byte.BYTES
      + Integer.BYTES
      + sourceDataLengthSize
      + sourceData.length
      + sessionInfo.packetSizeBytes();
    var informationBuffer = DataBufFactory.defaultFactory().createWithExpectedSize(transferBytes)
      .writeObject(sessionInfo)
      .writeInt(chunkIndex)
      .writeBoolean(false) // not the final chunk
      .writeByteArray(sourceData);
    return new ChunkedPacket(informationBuffer);
  }

  /**
   * Creates a new final chunk which must be sent to terminate a chunked data transfer on the remote side. Due to this
   * the provided chunk index is assumed to be the last index, which means that the total amount of chunks will be set
   * to {@code index + 1}. The final chunk of a transfer is allowed to contain fewer or no bytes than the chunk size of
   * the session. To properly serialize the smaller chunk data which can be provided in a bigger data byte array, the
   * given read bytes will be used to only serialize the chunk of bytes in the given array that are actually relevant.
   *
   * @param chunkIndex  the 0-based index of the chunk that is being sent.
   * @param readBytes   the amount of bytes that were read from the underlying source.
   * @param sourceData  the data that was read from the underlying source.
   * @param sessionInfo the information about the transfer session that this packet is related to.
   * @return the created chunk packet based on the information.
   * @throws NullPointerException if the given chunk information is null.
   */
  public static @NonNull ChunkedPacket createFinalChunk(
    int chunkIndex,
    int readBytes,
    byte[] sourceData,
    @NonNull ChunkSessionInformation sessionInfo
  ) {
    var sourceDataLengthSize = NettyUtil.varIntBytes(readBytes);
    var transferBytes = Byte.BYTES
      + Integer.BYTES
      + sourceDataLengthSize
      + readBytes
      + sessionInfo.packetSizeBytes();
    var informationBuffer = DataBufFactory.defaultFactory().createWithExpectedSize(transferBytes)
      .writeObject(sessionInfo)
      .writeInt(chunkIndex)
      .writeBoolean(true) // final chunk
      .writeByteArray(sourceData, readBytes);
    return new ChunkedPacket(informationBuffer);
  }
}
