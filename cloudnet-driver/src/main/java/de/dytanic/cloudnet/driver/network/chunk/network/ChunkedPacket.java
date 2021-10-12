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

package de.dytanic.cloudnet.driver.network.chunk.network;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkedPacket extends Packet {

  public ChunkedPacket(DataBuf dataBuf) {
    super(NetworkConstants.CHUNKED_PACKET_COM_CHANNEL, dataBuf);
  }

  public static @NotNull ChunkedPacket createChunk(
    @NotNull ChunkSessionInformation information,
    int chunkIndex,
    byte[] data
  ) {
    return createChunk(information, null, chunkIndex, data.length, data);
  }

  public static @NotNull ChunkedPacket createChunk(
    @NotNull ChunkSessionInformation information,
    @Nullable Integer chunkAmount,
    int chunkIndex,
    int dataLength,
    byte[] data
  ) {
    DataBuf.Mutable dataBuf = DataBuf.empty()
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
