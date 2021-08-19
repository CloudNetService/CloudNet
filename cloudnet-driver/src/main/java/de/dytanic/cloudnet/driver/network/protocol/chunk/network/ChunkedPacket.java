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

package de.dytanic.cloudnet.driver.network.protocol.chunk.network;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.chunk.data.ChunkSessionInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChunkedPacket extends Packet {

  public ChunkedPacket(DataBuf dataBuf) {
    super(PacketConstants.CHUNKED_PACKET_COM_CHANNEL, dataBuf);
  }

  public static @NotNull ChunkedPacket createChunk(@NotNull ChunkSessionInformation information, byte[] data) {
    return createChunk(information, null, data);
  }

  public static @NotNull ChunkedPacket createChunk(
    @NotNull ChunkSessionInformation information,
    @Nullable Integer chunkAmount,
    byte[] data
  ) {
    DataBuf.Mutable dataBuf = DataBufFactory.defaultFactory().createEmpty();
    // transfer information
    dataBuf
      .writeInt(information.getTransferType())
      .writeInt(information.getChunkSize())
      .writeUniqueId(information.getSessionUniqueId())
      .writeByteArray(information.getTransferInformation().toByteArray())
      // if the packet is the ending packet holding the information about the chunk amount
      .writeBoolean(chunkAmount != null);
    // if we know the chunk amount => write it
    if (chunkAmount != null) {
      dataBuf.writeInt(chunkAmount);
    }
    // write the actual content of the chunk
    return new ChunkedPacket(dataBuf.writeByteArray(data));
  }
}
