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

import com.google.common.base.Utf8;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufable;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Contains all needed information for a chunked data transfer to be initialized. The transfer information in this
 * object are there to allow writes of additional information needed for the transfer to work, for example the target
 * file name.
 *
 * @since 4.0
 */
public final class ChunkSessionInformation implements DataBufable {

  private int chunkSize;
  private UUID sessionUniqueId;
  private String transferChannel;
  private DataBuf transferInformation;

  /**
   * Used for network deserialization.
   */
  @ApiStatus.Internal
  public ChunkSessionInformation() {
  }

  /**
   * Constructs a new chunk session information.
   *
   * @param chunkSize           the size of data transferred in each chunk, should always be the exact amount of bytes.
   * @param sessionUniqueId     the unique id of the transfer session, for identification reasons.
   * @param transferChannel     the name of the channel the data is transferred in, for identification reasons.
   * @param transferInformation additional information for the transfer handler to handle the chunks correctly.
   * @throws NullPointerException if the given session id, transfer channel or transfer information buffer is null.
   */
  public ChunkSessionInformation(
    int chunkSize,
    @NonNull UUID sessionUniqueId,
    @NonNull String transferChannel,
    @NonNull DataBuf transferInformation
  ) {
    this.chunkSize = chunkSize;
    this.sessionUniqueId = sessionUniqueId;
    this.transferChannel = transferChannel;
    this.transferInformation = transferInformation;
  }

  /**
   * Returns the bytes required to write this session information into a buffer.
   *
   * @return the bytes required to write this session information into a buffer.
   */
  @ApiStatus.Internal
  public int packetSizeBytes() {
    var channelBytes = Utf8.encodedLength(this.transferChannel);
    var transferBytes = this.transferInformation.readableBytes();
    return Byte.BYTES              // nullable
      + Integer.BYTES              // chunk size
      + (Long.BYTES * 2)           // session id
      + 5                          // channel name (max length)
      + channelBytes
      + 5                          // extra transfer info (max length)
      + transferBytes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeData(@NonNull DataBuf.Mutable dataBuf) {
    dataBuf.writeInt(this.chunkSize);
    dataBuf.writeUniqueId(this.sessionUniqueId);
    dataBuf.writeString(this.transferChannel);
    dataBuf.writeDataBuf(this.transferInformation.acquire());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readData(@NonNull DataBuf dataBuf) {
    this.chunkSize = dataBuf.readInt();
    this.sessionUniqueId = dataBuf.readUniqueId();
    this.transferChannel = dataBuf.readString();
    this.transferInformation = dataBuf.readDataBuf();
  }

  /**
   * Get the size of a single chunk in the transfer.
   *
   * @return the size of a single chunk in the transfer.
   */
  public int chunkSize() {
    return this.chunkSize;
  }

  /**
   * Get the session unique id.
   *
   * @return the session unique id.
   */
  public @NonNull UUID sessionUniqueId() {
    return this.sessionUniqueId;
  }

  /**
   * Get the transfer channel identifier.
   *
   * @return the transfer channel identifier.
   */
  public @NonNull String transferChannel() {
    return this.transferChannel;
  }

  /**
   * Get the buffer containing additional information about the transfer.
   *
   * @return the buffer containing additional information about the transfer.
   */
  public @NonNull DataBuf transferInformation() {
    return this.transferInformation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof ChunkSessionInformation that)) {
      return false;
    } else {
      return this.sessionUniqueId.equals(that.sessionUniqueId());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.sessionUniqueId.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return "ChunkSessionInformation[" +
      "chunkSize=" + this.chunkSize + ", " +
      "sessionUniqueId=" + this.sessionUniqueId + ", " +
      "transferChannel=" + this.transferChannel + ", " +
      "transferInformation=" + this.transferInformation + ']';
  }
}
