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

package eu.cloudnetservice.driver.impl.network;

import eu.cloudnetservice.driver.impl.network.protocol.DefaultPacketListenerRegistry;
import eu.cloudnetservice.driver.impl.network.protocol.DefaultQueryPacketManager;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.protocol.QueryPacketManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;

/**
 * The default abstract implementation of a network channel.
 *
 * @since 4.0
 */
public abstract class DefaultNetworkChannel implements NetworkChannel {

  private static final AtomicLong CHANNEL_ID_COUNTER = new AtomicLong();

  private final long channelId = CHANNEL_ID_COUNTER.incrementAndGet();

  private final QueryPacketManager queryPacketManager;
  private final PacketListenerRegistry packetRegistry;

  private final HostAndPort serverAddress;
  private final HostAndPort clientAddress;

  private final boolean clientProvidedChannel;
  private final NetworkChannelHandler handler;

  /**
   * Constructs a new default network channel instance.
   *
   * @param packetRegistry        the packet registry to use for the channel.
   * @param serverAddress         the associated server address for the connection.
   * @param clientAddress         the associated client address for the connection.
   * @param clientProvidedChannel true if this channel was opened by a client, false otherwise.
   * @param handler               the handler to post underlying network events to.
   * @throws NullPointerException if one of the given arguments is null.
   */
  public DefaultNetworkChannel(
    @NonNull PacketListenerRegistry packetRegistry,
    @NonNull HostAndPort serverAddress,
    @NonNull HostAndPort clientAddress,
    boolean clientProvidedChannel,
    @NonNull NetworkChannelHandler handler
  ) {
    this.queryPacketManager = new DefaultQueryPacketManager(this);
    this.packetRegistry = new DefaultPacketListenerRegistry(packetRegistry);
    this.serverAddress = serverAddress;
    this.clientAddress = clientAddress;
    this.clientProvidedChannel = clientProvidedChannel;
    this.handler = handler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Packet> sendQueryAsync(@NonNull Packet packet) {
    return this.queryPacketManager.sendQueryPacket(packet);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long channelId() {
    return this.channelId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull PacketListenerRegistry packetRegistry() {
    return this.packetRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull QueryPacketManager queryPacketManager() {
    return this.queryPacketManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HostAndPort serverAddress() {
    return this.serverAddress;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HostAndPort clientAddress() {
    return this.clientAddress;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean clientProvidedChannel() {
    return this.clientProvidedChannel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull NetworkChannelHandler handler() {
    return this.handler;
  }
}
