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

package de.dytanic.cloudnet.driver.network.chunk;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.chunk.defaults.builder.FileChunkedPacketSenderBuilder;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NonNull;

public interface ChunkedPacketSender extends ChunkedPacketProvider {

  static @NonNull FileChunkedPacketSenderBuilder forFileTransfer() {
    return new FileChunkedPacketSenderBuilder();
  }

  @NonNull InputStream source();

  @NonNull Consumer<IPacket> packetSplitter();

  @NonNull ITask<TransferStatus> transferChunkedData();

  interface Builder {

    @NonNull Builder chunkSize(int chunkSize);

    @NonNull Builder sessionUniqueId(@NonNull UUID uuid);

    @NonNull Builder transferChannel(@NonNull String transferChannel);

    @NonNull Builder source(@NonNull InputStream source);

    @NonNull Builder toChannels(INetworkChannel @NonNull ... channels);

    @NonNull Builder toChannels(@NonNull Collection<INetworkChannel> channels);

    @NonNull Builder packetSplitter(@NonNull Consumer<IPacket> splitter);

    @NonNull Builder withExtraData(@NonNull DataBuf extraData);

    @NonNull ChunkedPacketSender build();
  }
}
