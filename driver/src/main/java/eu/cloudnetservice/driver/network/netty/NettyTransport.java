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

import eu.cloudnetservice.common.function.TriFunction;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelFactory;
import io.netty5.channel.EventLoop;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.IoHandlerFactory;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.channel.ServerChannel;
import io.netty5.channel.ServerChannelFactory;
import io.netty5.channel.epoll.Epoll;
import io.netty5.channel.epoll.EpollHandler;
import io.netty5.channel.epoll.EpollServerSocketChannel;
import io.netty5.channel.epoll.EpollSocketChannel;
import io.netty5.channel.kqueue.KQueue;
import io.netty5.channel.kqueue.KQueueHandler;
import io.netty5.channel.kqueue.KQueueServerSocketChannel;
import io.netty5.channel.kqueue.KQueueSocketChannel;
import io.netty5.channel.nio.NioHandler;
import io.netty5.channel.socket.nio.NioServerSocketChannel;
import io.netty5.channel.socket.nio.NioSocketChannel;
import java.net.ProtocolFamily;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * Holds all supported transport types and functionality to retrieve model instances for servers/clients construction.
 *
 * @since 4.0
 */
public enum NettyTransport {

  EPOLL(
    "epoll",
    Epoll.isAvailable(),
    true,
    EpollHandler::newFactory,
    EpollSocketChannel::new,
    EpollServerSocketChannel::new
  ),
  KQUEUE(
    "kqueue",
    KQueue.isAvailable(),
    true,
    KQueueHandler::newFactory,
    KQueueSocketChannel::new,
    KQueueServerSocketChannel::new
  ),
  NIO(
    "nio",
    true,
    false,
    NioHandler::newFactory,
      (eventLoop, protocolFamily) -> new NioSocketChannel(eventLoop),
      (eventLoop, eventLoopGroup, protocolFamily) -> new NioServerSocketChannel(eventLoop, eventLoopGroup)
  );

  private final String name;
  private final boolean available;
  private final boolean nativeTransport;
  private final Supplier<IoHandlerFactory> ioHandlerFactory;
  private final BiFunction<EventLoop, ProtocolFamily, ? extends Channel> clientChannelFactory;
  private final TriFunction<EventLoop, EventLoopGroup, ProtocolFamily, ? extends ServerChannel> serverChannelFactory;
  
  /**
   * Constructs a new netty transport instance.
   *
   * @param name                 the display name of the transport.
   * @param available            if the transport is available.
   * @param nativeTransport      if the transport is native.
   * @param ioHandlerFactory     the factory for io handlers.
   * @param clientChannelFactory the factory for client channels.
   * @param serverChannelFactory the factory for server channels.
   * @throws NullPointerException if one of the given parameters is null.
   */
  NettyTransport(
    @NonNull String name,
    boolean available,
    boolean nativeTransport,
    @NonNull Supplier<IoHandlerFactory> ioHandlerFactory,
    @NonNull BiFunction<EventLoop, ProtocolFamily, ? extends Channel> clientChannelFactory,
    @NonNull TriFunction<EventLoop, EventLoopGroup, ProtocolFamily, ? extends ServerChannel> serverChannelFactory
  ) {
    this.name = name;
    this.available = available;
    this.nativeTransport = nativeTransport;
    this.ioHandlerFactory = ioHandlerFactory;
    this.clientChannelFactory = clientChannelFactory;
    this.serverChannelFactory = serverChannelFactory;
  }

  /**
   * Selects and returns the first available transport. If this method should not return native transports, it currently
   * only returns nio.
   *
   * @param noNative if no native transport should get included into the selection.
   * @return the first available transport.
   * @throws IllegalStateException if no transport is available, should normally never happen.
   */
  public static @NonNull NettyTransport availableTransport(boolean noNative) {
    for (var transport : values()) {
      // ignore native transports if no-native is selected
      if (noNative && transport.nativeTransport()) {
        continue;
      }

      // use the first available transport
      if (transport.available) {
        return transport;
      }
    }
    // unable to find a transport?
    throw new IllegalStateException("Unable to select an available netty transport!");
  }

  /**
   * Creates a new event loop group of the current selected transport with the supplied amount of threads.
   *
   * @param threads the amount of threads.
   * @return a new event loop group for this transport.
   * @throws IllegalArgumentException if the given number of threads is negative.
   */
  public @NonNull EventLoopGroup createEventLoopGroup(int threads) {
    return new MultithreadEventLoopGroup(threads, this.ioHandlerFactory.get());
  }

  /**
   * Get the display name of the transport.
   *
   * @return the display name.
   */
  public @NonNull String displayName() {
    return this.name;
  }

  /**
   * Get if this transport type is native.
   *
   * @return if this transport type is native.
   */
  public boolean nativeTransport() {
    return this.nativeTransport;
  }

  /**
   * Get the factory for client channels of this transport.
   *
   * @param protocolFamily the protocol Family the channel should created for.
   * @return the factory for client channels of this transport.
   */
  public @NonNull ChannelFactory<? extends Channel> clientChannelFactory(@NonNull ProtocolFamily protocolFamily) {
    return (eventLoop) -> this.clientChannelFactory.apply(eventLoop, protocolFamily);
  }

  /**
   * Get the factory for server channels of this transport.
   *
   * @param protocolFamily the protocol Family the channel should created for.
   * @return the factory for server channels of this transport.
   */
  public @NonNull ServerChannelFactory<? extends ServerChannel> serverChannelFactory(@NonNull ProtocolFamily protocolFamily) {
    return (eventLoop, eventLoopGroup) -> this.serverChannelFactory.apply(eventLoop, eventLoopGroup, protocolFamily);
  }
}
