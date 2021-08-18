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

package de.dytanic.cloudnet.driver.network.netty.client;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.DefaultNetworkComponent;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.network.protocol.defaults.DefaultPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListenerRegistry;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.NotNull;

public class NettyNetworkClient implements DefaultNetworkComponent, INetworkClient {

  private static final int CONNECTION_TIMEOUT_MILLIS = 5_000;

  protected final Executor packetDispatcher = NettyUtils.newPacketDispatcher();
  protected final EventLoopGroup eventLoopGroup = NettyUtils.newEventLoopGroup();

  protected final Collection<INetworkChannel> channels = new ConcurrentLinkedQueue<>();
  protected final IPacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

  protected final SSLConfiguration sslConfiguration;
  protected final Callable<INetworkChannelHandler> networkChannelHandler;

  protected SslContext sslContext;

  public NettyNetworkClient(Callable<INetworkChannelHandler> networkChannelHandler) {
    this(networkChannelHandler, null);
  }

  public NettyNetworkClient(Callable<INetworkChannelHandler> networkChannelHandler, SSLConfiguration sslConfiguration) {
    this.networkChannelHandler = networkChannelHandler;
    this.sslConfiguration = sslConfiguration;

    try {
      this.init();
    } catch (Exception exception) {
      LOGGER.severe("Exception while initializing the netty network client", exception);
    }
  }

  @Override
  public boolean isSslEnabled() {
    return this.sslContext != null;
  }

  @Override
  public boolean connect(@NotNull HostAndPort hostAndPort) {
    Preconditions.checkNotNull(hostAndPort);
    Preconditions.checkNotNull(hostAndPort.getHost());

    try {
      new Bootstrap()
        .group(this.eventLoopGroup)
        .channelFactory(NettyUtils.getClientChannelFactory())
        .handler(new NettyNetworkClientInitializer(hostAndPort, this))

        .option(ChannelOption.AUTO_READ, true)
        .option(ChannelOption.IP_TOS, 24)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MILLIS)

        .connect(hostAndPort.getHost(), hostAndPort.getPort())

        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
        .syncUninterruptibly();

      return true;
    } catch (Exception exception) {
      LOGGER.severe(String.format("Exception while opening network connection to %s", hostAndPort), exception);
    }

    return false;
  }

  @Override
  public void close() {
    this.closeChannels();
    this.eventLoopGroup.shutdownGracefully();
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
  public @NotNull IPacketListenerRegistry getPacketRegistry() {
    return this.packetRegistry;
  }

  private void init() throws Exception {
    if (this.sslConfiguration != null && this.sslConfiguration.isEnabled()) {
      if (this.sslConfiguration.getCertificate() != null && this.sslConfiguration.getPrivateKey() != null) {
        SslContextBuilder builder = SslContextBuilder.forClient();

        if (this.sslConfiguration.getTrustCertificate() != null) {
          try (InputStream stream = Files.newInputStream(this.sslConfiguration.getTrustCertificate())) {
            builder.trustManager(stream);
          }
        } else {
          builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }

        try (InputStream cert = Files.newInputStream(this.sslConfiguration.getCertificate());
          InputStream privateKey = Files.newInputStream(this.sslConfiguration.getPrivateKey())) {
          this.sslContext = builder
            .keyManager(cert, privateKey)
            .clientAuth(this.sslConfiguration.isClientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL)
            .build();
        }
      } else {
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        this.sslContext = SslContextBuilder.forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .keyManager(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
          .build();
      }
    }
  }
}
