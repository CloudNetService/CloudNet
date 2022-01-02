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

package eu.cloudnetservice.cloudnet.driver.network.chunk.defaults.builder;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.cloudnet.driver.network.chunk.defaults.splitter.NetworkChannelsPacketSplitter;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NonNull;

public abstract class DefaultChunkedPacketSenderBuilder implements ChunkedPacketSender.Builder {

  public static final int DEFAULT_CHUNK_SIZE = 1024 * 1024;

  protected InputStream source;
  protected String transferChannel;
  protected Consumer<Packet> packetSplitter;

  protected int chunkSize = DEFAULT_CHUNK_SIZE;
  protected UUID sessionUniqueId = UUID.randomUUID();
  protected DataBuf transferInformation = DataBuf.empty();

  @Override
  public @NonNull ChunkedPacketSender.Builder chunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  @Override
  public @NonNull ChunkedPacketSender.Builder sessionUniqueId(@NonNull UUID uuid) {
    this.sessionUniqueId = uuid;
    return this;
  }

  @Override
  public @NonNull ChunkedPacketSender.Builder transferChannel(@NonNull String transferChannel) {
    this.transferChannel = transferChannel;
    return this;
  }

  @Override
  public @NonNull ChunkedPacketSender.Builder source(@NonNull InputStream source) {
    this.source = source;
    return this;
  }

  @Override
  public @NonNull ChunkedPacketSender.Builder toChannels(NetworkChannel @NonNull ... channels) {
    return this.toChannels(Arrays.asList(channels));
  }

  @Override
  public @NonNull ChunkedPacketSender.Builder toChannels(@NonNull Collection<NetworkChannel> channels) {
    return this.packetSplitter(new NetworkChannelsPacketSplitter(channels));
  }

  @Override
  public @NonNull ChunkedPacketSender.Builder packetSplitter(@NonNull Consumer<Packet> splitter) {
    this.packetSplitter = splitter;
    return this;
  }

  @Override
  public @NonNull ChunkedPacketSender.Builder withExtraData(@NonNull DataBuf extraData) {
    this.transferInformation = extraData.disableReleasing();
    return this;
  }

  @Override
  public @NonNull ChunkedPacketSender build() {
    Verify.verifyNotNull(this.source, "no source given to send");
    Verify.verifyNotNull(this.packetSplitter, "no packet splitter provided");
    Verify.verifyNotNull(this.transferChannel, "no transfer channel provided");
    Verify.verifyNotNull(this.sessionUniqueId, "no session unique id provided");
    Verify.verify(this.chunkSize > 0, "chunk size must be more than 0");

    return this.doBuild();
  }

  protected abstract @NonNull ChunkedPacketSender doBuild();
}
