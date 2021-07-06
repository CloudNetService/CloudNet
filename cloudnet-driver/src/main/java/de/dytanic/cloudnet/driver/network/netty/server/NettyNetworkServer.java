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
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.driver.network.DefaultNetworkComponent;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.INetworkServer;
import de.dytanic.cloudnet.driver.network.netty.NettySSLServer;
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.network.protocol.DefaultPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class NettyNetworkServer extends NettySSLServer implements DefaultNetworkComponent, INetworkServer {

  protected final Collection<INetworkChannel> channels = new ConcurrentLinkedQueue<>();
  protected final Map<Integer, Pair<HostAndPort, ChannelFuture>> channelFutures = new ConcurrentHashMap<>();

  protected final IPacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

  protected final Executor packetDispatcher = NettyUtils.newPacketDispatcher();

  protected final EventLoopGroup bossEventLoopGroup = NettyUtils.newEventLoopGroup();
  protected final EventLoopGroup workerEventLoopGroup = NettyUtils.newEventLoopGroup();

  protected final Callable<INetworkChannelHandler> networkChannelHandler;

  public NettyNetworkServer(Callable<INetworkChannelHandler> networkChannelHandler) {
    this(null, networkChannelHandler);
  }

  public NettyNetworkServer(SSLConfiguration sslConfiguration, Callable<INetworkChannelHandler> networkChannelHandler) {
    super(sslConfiguration);
    this.networkChannelHandler = networkChannelHandler;

    try {
      this.init();
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  @Deprecated
  public NettyNetworkServer(Callable<INetworkChannelHandler> networkChannelHandler, ITaskScheduler taskScheduler) {
    this(null, networkChannelHandler);
  }

  @Deprecated
  public NettyNetworkServer(Callable<INetworkChannelHandler> networkChannelHandler, SSLConfiguration sslConfiguration,
    ITaskScheduler taskScheduler) {
    this(sslConfiguration, networkChannelHandler);
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
  public boolean addListener(@NotNull HostAndPort hostAndPort) {
    Preconditions.checkNotNull(hostAndPort);
    Preconditions.checkNotNull(hostAndPort.getHost());

    if (!this.channelFutures.containsKey(hostAndPort.getPort())) {
      try {
        this.channelFutures.put(hostAndPort.getPort(), new Pair<>(hostAndPort, new ServerBootstrap()
          .group(this.bossEventLoopGroup, this.workerEventLoopGroup)
          .childOption(ChannelOption.TCP_NODELAY, true)
          .childOption(ChannelOption.IP_TOS, 24)
          .childOption(ChannelOption.AUTO_READ, true)
          .channelFactory(NettyUtils.getServerChannelFactory())
          .childHandler(new NettyNetworkServerInitializer(this, hostAndPort))
          .bind(hostAndPort.getHost(), hostAndPort.getPort())
          .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
          .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
          .sync()
          .channel()
          .closeFuture()));

        return true;
      } catch (InterruptedException exception) {
        exception.printStackTrace();
      }
    }

    return false;
  }

  @Override
  public void close() {
    this.closeChannels();

    for (Pair<HostAndPort, ChannelFuture> entry : this.channelFutures.values()) {
      entry.getSecond().cancel(true);
    }

    this.bossEventLoopGroup.shutdownGracefully();
    this.workerEventLoopGroup.shutdownGracefully();
  }

  public Collection<INetworkChannel> getChannels() {
    return Collections.unmodifiableCollection(this.channels);
  }

  @Override
  public Executor getPacketDispatcher() {
    return this.packetDispatcher;
  }

  @Override
  public Collection<INetworkChannel> getModifiableChannels() {
    return this.channels;
  }

  @Override
  public void sendPacketSync(@NotNull IPacket... packets) {
    Preconditions.checkNotNull(packets);

    for (INetworkChannel channel : this.channels) {
      channel.sendPacketSync(packets);
    }
  }

  public IPacketListenerRegistry getPacketRegistry() {
    return this.packetRegistry;
  }
}
