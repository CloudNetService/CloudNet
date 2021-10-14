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
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.chunk.defaults.builder.FileChunkedPacketSenderBuilder;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public interface ChunkedPacketSender extends ChunkedPacketProvider {

  static @NotNull FileChunkedPacketSenderBuilder forFileTransfer() {
    return new FileChunkedPacketSenderBuilder();
  }

  @NotNull InputStream getSource();

  @NotNull Consumer<IPacket> getChunkPacketSplitter();

  @NotNull ITask<TransferStatus> transferChunkedData();

  interface Builder {

    @NotNull Builder chunkSize(int chunkSize);

    @NotNull Builder sessionUniqueId(@NotNull UUID uuid);

    @NotNull Builder transferChannel(@NotNull String transferChannel);

    @NotNull Builder source(@NotNull InputStream source);

    @NotNull Builder toChannels(INetworkChannel @NotNull ... channels);

    @NotNull Builder toChannels(@NotNull Collection<INetworkChannel> channels);

    @NotNull Builder packetSplitter(@NotNull Consumer<IPacket> splitter);

    @NotNull Builder withExtraData(@NotNull JsonDocument extraData);

    @NotNull ChunkedPacketSender build();
  }
}
