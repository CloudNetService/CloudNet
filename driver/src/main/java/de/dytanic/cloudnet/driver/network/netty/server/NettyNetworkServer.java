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

package de.dytanic.cloudnet.driver.network.netty.server;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.network.DefaultNetworkComponent;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.netty.NettySSLServer;
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.defaults.DefaultPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.NotNull;

public class NettyNetworkServer extends NettySSLServer implements DefaultNetworkComponent, INetworkServer {

  protected static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(1 << 20, 1 << 21);

  protected final EventLoopGroup bossEventLoopGroup = NettyUtils.newEventLoopGroup();
  protected final EventLoopGroup workerEventLoopGroup = NettyUtils.newEventLoopGroup();

  protected final Collection<INetworkChannel> channels = new ConcurrentLinkedQueue<>();
  protected final Map<Integer, Pair<HostAndPort, ChannelFuture>> channelFutures = new ConcurrentHashMap<>();

  protected final Executor packetDispatcher = NettyUtils.newPacketDispatcher();
  protected final IPacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

  protected final Callable<INetworkChannelHandler> networkChannelHandlerFactory;

  public NettyNetworkServer(Callable<INetworkChannelHandler> networkChannelHandler) {
    this(networkChannelHandler, null);
  }

  public NettyNetworkServer(Callable<INetworkChannelHandler> networkChannelHandler, SSLConfiguration sslConfiguration) {
    super(sslConfiguration);

    this.networkChannelHandlerFactory = networkChannelHandler;

    try {
      this.init();
    } catch (Exception exception) {
      LOGGER.severe("Exception while initializing the netty network server", exception);
    }
  }

  @Override
  public boolean isSslEnabled() {
    return this.sslContext != null;
  }

  @Override
  public boolean addListener(int port) {
    return this.addListener(new HostAndPort("0.0.0.0", port));
  }

  @Override
  public boolean addListener(@NotNull SocketAddress socketAddress) {
    return this.addListener(HostAndPort.fromSocketAddress(socketAddress));
  }

  @Override
  public boolean addListener(@NotNull HostAndPort hostAndPort) {
    Preconditions.checkNotNull(hostAndPort);
    Preconditions.checkNotNull(hostAndPort.getHost());

    // check if a server is already bound to the port
    if (!this.channelFutures.containsKey(hostAndPort.getPort())) {
      try {
        // create the server
        var bootstrap = new ServerBootstrap()
          .channelFactory(NettyUtils.getServerChannelFactory())
          .group(this.bossEventLoopGroup, this.workerEventLoopGroup)
          .childHandler(new NettyNetworkServerInitializer(this, hostAndPort))

          .childOption(ChannelOption.IP_TOS, 0x18)
          .childOption(ChannelOption.AUTO_READ, true)
          .childOption(ChannelOption.TCP_NODELAY, true)
          .childOption(ChannelOption.SO_KEEPALIVE, true)
          .childOption(ChannelOption.SO_REUSEADDR, true)
          .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK);
        // check if tcp fast open is supported
        if (NettyUtils.NATIVE_TRANSPORT) {
          bootstrap.option(ChannelOption.TCP_FASTOPEN, 0x3);
        }
        // register the server and bind it
        return this.channelFutures.putIfAbsent(hostAndPort.getPort(), new Pair<>(hostAndPort, bootstrap
          .bind(hostAndPort.getHost(), hostAndPort.getPort())
          .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
          .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)

          .sync()
          .channel()
          .closeFuture())) == null;
      } catch (InterruptedException exception) {
        LOGGER.severe("Exception binding network server instance to %s", exception, hostAndPort);
      }
    }

    return false;
  }

  @Override
  public void close() {
    this.closeChannels();

    for (var entry : this.channelFutures.values()) {
      entry.getSecond().cancel(true);
    }

    this.bossEventLoopGroup.shutdownGracefully();
    this.workerEventLoopGroup.shutdownGracefully();
  }

  @Override
  public @NotNull Collection<INetworkChannel> getChannels() {
    return Collections.unmodifiableCollection(this.channels);
  }

  @Override
  public @NotNull Executor getPacketDispatcher() {
    return this.packetDispatcher;
  }

  @Override
  public Collection<INetworkChannel> getModifiableChannels() {
    return this.channels;
  }

  @Override
  public void sendPacketSync(@NotNull IPacket... packets) {
    Preconditions.checkNotNull(packets);

    for (var channel : this.channels) {
      channel.sendPacketSync(packets);
    }
  }

  @Override
  public @NotNull IPacketListenerRegistry getPacketRegistry() {
    return this.packetRegistry;
  }
}
