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

package eu.cloudnetservice.driver.network.netty.server;

import eu.cloudnetservice.driver.network.DefaultNetworkComponent;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.network.netty.NettySslServer;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.protocol.defaults.DefaultPacketListenerRegistry;
import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.WriteBufferWaterMark;
import io.netty5.util.concurrent.Future;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty based implementation of the network server.
 *
 * @since 4.0
 */
public class NettyNetworkServer extends NettySslServer implements DefaultNetworkComponent, NetworkServer {

  protected static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(1 << 20, 1 << 21);

  protected final EventLoopGroup bossEventLoopGroup = NettyUtil.newEventLoopGroup(1);
  protected final EventLoopGroup workerEventLoopGroup = NettyUtil.newEventLoopGroup(0);

  protected final Collection<NetworkChannel> channels = new ConcurrentLinkedQueue<>();
  protected final Map<HostAndPort, Future<Void>> channelFutures = new ConcurrentHashMap<>();

  protected final Executor packetDispatcher = NettyUtil.newPacketDispatcher();
  protected final PacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

  protected final Callable<NetworkChannelHandler> handlerFactory;

  /**
   * Constructs a new netty network server instance. Equivalent to {@code new NettyNetworkServer(factory, null)}.
   *
   * @param handlerFactory the handler factory to use.
   * @throws NullPointerException if the given handler factory is null.
   */
  public NettyNetworkServer(@NonNull Callable<NetworkChannelHandler> handlerFactory) {
    this(handlerFactory, null);
  }

  /**
   * Constructs a new netty network server instance.
   *
   * @param handlerFactory   the handler factory to use.
   * @param sslConfiguration the ssl configuration to apply, or null if ssl should be disabled.
   * @throws NullPointerException if the given handler factory is null.
   */
  public NettyNetworkServer(
    @NonNull Callable<NetworkChannelHandler> handlerFactory,
    @Nullable SSLConfiguration sslConfiguration
  ) {
    super(sslConfiguration);
    this.handlerFactory = handlerFactory;

    try {
      this.init();
    } catch (Exception exception) {
      LOGGER.severe("Exception while initializing the netty network server", exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean sslEnabled() {
    return this.sslContext != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addListener(int port) {
    return this.addListener(new HostAndPort("0.0.0.0", port));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addListener(@NonNull HostAndPort hostAndPort) {
    // check if a server is already bound to the port
    if (!this.channelFutures.containsKey(hostAndPort)) {
      try {
        // create the server
        var bootstrap = new ServerBootstrap()
          .channelFactory(NettyUtil.serverChannelFactory())
          .group(this.bossEventLoopGroup, this.workerEventLoopGroup)
          .childHandler(new NettyNetworkServerInitializer(this, hostAndPort))

          .childOption(ChannelOption.IP_TOS, 0x18)
          .childOption(ChannelOption.AUTO_READ, true)
          .childOption(ChannelOption.TCP_NODELAY, true)
          .childOption(ChannelOption.SO_KEEPALIVE, true)
          .childOption(ChannelOption.SO_REUSEADDR, true)
          .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK);
        // register the server and bind it
        this.channelFutures.putIfAbsent(hostAndPort, bootstrap
          .bind(hostAndPort.host(), hostAndPort.port())
          .sync()
          .getNow()
          .closeFuture());
        return true;
      } catch (Exception exception) {
        LOGGER.severe("Exception binding network server instance to %s", exception, hostAndPort);
      }
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.closeChannels();

    for (var entry : this.channelFutures.values()) {
      entry.cancel();
    }

    this.bossEventLoopGroup.shutdownGracefully();
    this.workerEventLoopGroup.shutdownGracefully();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<NetworkChannel> channels() {
    return Collections.unmodifiableCollection(this.channels);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Executor packetDispatcher() {
    return this.packetDispatcher;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<NetworkChannel> modifiableChannels() {
    return this.channels;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacketSync(@NonNull Packet... packets) {
    for (var channel : this.channels) {
      channel.sendPacketSync(packets);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull PacketListenerRegistry packetRegistry() {
    return this.packetRegistry;
  }
}
