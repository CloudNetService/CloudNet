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

package eu.cloudnetservice.driver.impl.network.netty;

import eu.cloudnetservice.driver.impl.network.DefaultNetworkChannel;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import io.netty5.channel.Channel;
import io.netty5.util.concurrent.Promise;
import io.netty5.util.concurrent.PromiseCombiner;
import lombok.NonNull;

/**
 * The default netty based implementation of a network channel.
 *
 * @since 4.0
 */
public final class NettyNetworkChannel extends DefaultNetworkChannel implements NetworkChannel {

  private final Channel channel;

  /**
   * Constructs a new netty network channel instance.
   *
   * @param channel               the netty channel to wrap.
   * @param packetRegistry        the packet registry for this channel.
   * @param handler               the handler to post events to.
   * @param serverAddress         the server address to which the client connected.
   * @param clientAddress         the client address which is assigned to the connection.
   * @param clientProvidedChannel true if the channel is handled by a server, false otherwise.
   * @throws NullPointerException if one of the required constructor paramters is null.
   */
  public NettyNetworkChannel(
    @NonNull Channel channel,
    @NonNull PacketListenerRegistry packetRegistry,
    @NonNull NetworkChannelHandler handler,
    @NonNull HostAndPort serverAddress,
    @NonNull HostAndPort clientAddress,
    boolean clientProvidedChannel
  ) {
    super(packetRegistry, serverAddress, clientAddress, clientProvidedChannel, handler);
    this.channel = channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacket(@NonNull Packet... packets) {
    var executor = this.channel.executor();
    if (executor.inEventLoop()) {
      // on event loop, start all write operations
      var combiner = new PromiseCombiner(executor);
      for (var packet : packets) {
        var writeFuture = this.channel.write(packet);
        combiner.add(writeFuture);
      }

      // wait for all write operations to complete and flush the content afterwards
      Promise<Void> promise = this.channel.newPromise();
      combiner.finish(promise);
      promise.asFuture().addListener(_ -> this.channel.flush());
    } else {
      // this has to be called from event loop as promise combiner is not thread safe
      executor.execute(() -> this.sendPacket(packets));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacket(@NonNull Packet packet) {
    this.channel.writeAndFlush(packet);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacketSync(@NonNull Packet packet) {
    var future = this.channel.writeAndFlush(packet);
    if (!future.executor().inEventLoop()) {
      // only await the future if we're not currently in the event loop
      // as this would deadlock the write operations triggered previously
      try {
        future.asStage().await();
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt(); // reset interrupted state
      }
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
}
