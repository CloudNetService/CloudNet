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

package eu.cloudnetservice.driver.network.netty;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelPacketSendEvent;
import eu.cloudnetservice.driver.network.DefaultNetworkChannel;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import io.netty5.channel.Channel;
import io.netty5.util.concurrent.Future;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty based implementation of a network channel.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class NettyNetworkChannel extends DefaultNetworkChannel implements NetworkChannel {

  private final Channel channel;
  private final EventManager eventManager;

  /**
   * Constructs a new netty network channel instance.
   *
   * @param channel               the netty channel to wrap.
   * @param eventManager          the event manager for the current component.
   * @param packetRegistry        the packet registry for this channel.
   * @param handler               the handler to post events to.
   * @param serverAddress         the server address to which the client connected.
   * @param clientAddress         the client address which is assigned to the connection.
   * @param clientProvidedChannel true if the channel is handled by a server, false otherwise.
   * @throws NullPointerException if one of the required constructor paramters is null.
   */
  public NettyNetworkChannel(
    @NonNull Channel channel,
    @NonNull EventManager eventManager,
    @NonNull PacketListenerRegistry packetRegistry,
    @NonNull NetworkChannelHandler handler,
    @NonNull HostAndPort serverAddress,
    HostAndPort clientAddress,
    boolean clientProvidedChannel
  ) {
    super(packetRegistry, serverAddress, clientAddress, clientProvidedChannel, handler);
    this.channel = channel;
    this.eventManager = eventManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacket(@NonNull Packet... packets) {
    for (var packet : packets) {
      this.writePacket(packet, false);
    }
    this.channel.flush(); // reduces i/o load
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacketSync(@NonNull Packet... packets) {
    for (var packet : packets) {
      var future = this.writePacket(packet, false);
      if (future != null) {
        NettyUtil.awaitFuture(future);
      }
    }
    this.channel.flush(); // reduces i/o load
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacket(@NonNull Packet packet) {
    if (this.channel.executor().inEventLoop()) {
      this.writePacket(packet, true);
    } else {
      this.channel.executor().execute(() -> this.writePacket(packet, true));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacketSync(@NonNull Packet packet) {
    var future = this.writePacket(packet, true);
    if (future != null) {
      NettyUtil.awaitFuture(future);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeable() {
    return this.channel.isWritable();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean active() {
    return this.channel.isActive();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.channel.close();
  }

  /**
   * Writes the given packet into the channel, calling the packet send event beforehand and not writing when the event
   * gets cancelled by a module/plugin.
   *
   * @param packet     the packet to write if the send operation is not cancelled.
   * @param flushAfter if the send queue should be flushed directly after the write process.
   * @return the future completed once the write operation (and flush) of the channel succeeded, null if cancelled.
   * @throws NullPointerException if the given packet is null.
   */
  private @Nullable Future<Void> writePacket(@NonNull Packet packet, boolean flushAfter) {
    var event = this.eventManager.callEvent(new NetworkChannelPacketSendEvent(this, packet));
    if (!event.cancelled()) {
      return flushAfter ? this.channel.writeAndFlush(packet) : this.channel.write(packet);
    } else {
      return null;
    }
  }
}
