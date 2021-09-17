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

package de.dytanic.cloudnet.driver.network.chunk.defaults.builder;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketSender;
import de.dytanic.cloudnet.driver.network.chunk.defaults.splitter.NetworkChannelsPacketSplitter;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public abstract class DefaultChunkedPacketSenderBuilder implements ChunkedPacketSender.Builder {

  public static final int DEFAULT_CHUNK_SIZE = 1024 * 1024;

  protected int transferType;
  protected InputStream source;
  protected Consumer<IPacket> packetSplitter;

  protected int chunkSize = DEFAULT_CHUNK_SIZE;
  protected UUID sessionUniqueId = UUID.randomUUID();
  protected JsonDocument transferInformation = JsonDocument.newDocument();

  @Override
  public @NotNull ChunkedPacketSender.Builder chunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  @Override
  public @NotNull ChunkedPacketSender.Builder sessionUniqueId(@NotNull UUID uuid) {
    this.sessionUniqueId = uuid;
    return this;
  }

  @Override
  public @NotNull ChunkedPacketSender.Builder transferMode(int transferMode) {
    this.transferType = transferMode;
    return this;
  }

  @Override
  public @NotNull ChunkedPacketSender.Builder source(@NotNull InputStream source) {
    this.source = source;
    return this;
  }

  @Override
  public @NotNull ChunkedPacketSender.Builder toChannels(INetworkChannel @NotNull ... channels) {
    return this.toChannels(Arrays.asList(channels));
  }

  @Override
  public @NotNull ChunkedPacketSender.Builder toChannels(@NotNull Collection<INetworkChannel> channels) {
    return this.packetSplitter(new NetworkChannelsPacketSplitter(channels));
  }

  @Override
  public @NotNull ChunkedPacketSender.Builder packetSplitter(@NotNull Consumer<IPacket> splitter) {
    this.packetSplitter = splitter;
    return this;
  }

  @Override
  public @NotNull ChunkedPacketSender.Builder withExtraData(@NotNull JsonDocument extraData) {
    this.transferInformation.append(extraData);
    return this;
  }

  @Override
  public @NotNull ChunkedPacketSender build() {
    Verify.verifyNotNull(this.source, "no source given to send");
    Verify.verifyNotNull(this.packetSplitter, "no packet splitter provided");
    Verify.verifyNotNull(this.sessionUniqueId, "no session unique id provided");
    Verify.verify(this.chunkSize > 0, "chunk size must be more than 0");

    return this.doBuild();
  }

  protected abstract @NotNull ChunkedPacketSender doBuild();
}
