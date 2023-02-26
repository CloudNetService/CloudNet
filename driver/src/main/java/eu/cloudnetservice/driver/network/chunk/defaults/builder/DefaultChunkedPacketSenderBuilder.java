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

package eu.cloudnetservice.driver.network.chunk.defaults.builder;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.defaults.splitter.NetworkChannelsPacketSplitter;
import eu.cloudnetservice.driver.network.protocol.Packet;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * Represents a default abstract implementation of a chunked packet sender builder for other types of transfer to
 * expand.
 *
 * @see FileChunkedPacketSenderBuilder
 * @see ChunkedPacketSender#forFileTransfer()
 * @since 4.0
 */
public abstract class DefaultChunkedPacketSenderBuilder implements ChunkedPacketSender.Builder {

  public static final int DEFAULT_CHUNK_SIZE = 50 * 1024 * 1024;

  protected InputStream source;
  protected String transferChannel;
  protected Consumer<Packet> packetSplitter;

  protected int chunkSize = DEFAULT_CHUNK_SIZE;
  protected UUID sessionUniqueId = UUID.randomUUID();
  protected DataBuf transferInformation = DataBuf.empty();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender.Builder chunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender.Builder sessionUniqueId(@NonNull UUID uuid) {
    this.sessionUniqueId = uuid;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender.Builder transferChannel(@NonNull String transferChannel) {
    this.transferChannel = transferChannel;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender.Builder source(@NonNull InputStream source) {
    this.source = source;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender.Builder toChannels(NetworkChannel @NonNull ... channels) {
    return this.toChannels(Arrays.asList(channels));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender.Builder toChannels(@NonNull Collection<NetworkChannel> channels) {
    return this.packetSplitter(new NetworkChannelsPacketSplitter(channels));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender.Builder packetSplitter(@NonNull Consumer<Packet> splitter) {
    this.packetSplitter = splitter;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender.Builder withExtraData(@NonNull DataBuf extraData) {
    // make sure that the old transfer information buffer is released
    this.transferInformation.release();

    this.transferInformation = extraData;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedPacketSender build() {
    Preconditions.checkNotNull(this.source, "no source given to send");
    Preconditions.checkNotNull(this.packetSplitter, "no packet splitter provided");
    Preconditions.checkNotNull(this.transferChannel, "no transfer channel provided");
    Preconditions.checkNotNull(this.sessionUniqueId, "no session unique id provided");
    Preconditions.checkArgument(this.chunkSize > 0, "chunk size must be more than 0");

    return this.doBuild();
  }

  /**
   * Delegate method which must get overridden by any class which extends from this builder. The method is used to
   * actually build the sender while being sure that there are no violations to the build contract (the delegation
   * method should, but must not be assertion free).
   *
   * @return the sender instance build based on the information supplied to this builder.
   * @throws NullPointerException if any assertions must be made separately and fails.
   */
  protected abstract @NonNull ChunkedPacketSender doBuild();
}
