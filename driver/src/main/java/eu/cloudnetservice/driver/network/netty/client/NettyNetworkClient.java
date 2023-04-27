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

package eu.cloudnetservice.driver.network.netty.client;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.DefaultNetworkComponent;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.protocol.defaults.DefaultPacketListenerRegistry;
import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.WriteBufferWaterMark;
import io.netty5.handler.ssl.ClientAuth;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a network client, using the netty network client implementations.
 *
 * @since 4.0
 */
@Singleton
public class NettyNetworkClient implements DefaultNetworkComponent, NetworkClient {

  private static final int CONNECTION_TIMEOUT_MILLIS = 5_000;
  private static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(1 << 20, 1 << 21);

  protected final EventLoopGroup eventLoopGroup = NettyUtil.newEventLoopGroup(0);

  protected final Collection<NetworkChannel> channels = ConcurrentHashMap.newKeySet();
  protected final PacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

  protected final EventManager eventManager;
  protected final Executor packetDispatcher;
  protected final SSLConfiguration sslConfiguration;
  protected final Callable<NetworkChannelHandler> handlerFactory;

  protected SslContext sslContext;

  /**
   * Constructs a new netty network client instance. Equivalent to {@code new NettyNetworkClient(handlerFactory, null)}
   *
   * @param eventManager   the event manager of the current component.
   * @param componentInfo  the component info of the current component the client is created for.
   * @param handlerFactory the factory for new handlers to be created with.
   * @throws NullPointerException if the given event manager, component info or factory is null.
   */
  public NettyNetworkClient(
    @NonNull EventManager eventManager,
    @NonNull ComponentInfo componentInfo,
    @NonNull Callable<NetworkChannelHandler> handlerFactory
  ) {
    this(eventManager, componentInfo, handlerFactory, null);
  }

  /**
   * Constructs a new netty network client instance.
   *
   * @param eventManager     the event manager of the current component.
   * @param componentInfo    the component info of the current component the client is created for.
   * @param handlerFactory   the factory for new handler to be created with.
   * @param sslConfiguration the ssl configuration applying to this client, or null for no ssl configuration.
   * @throws NullPointerException if the given event manager, component info or factory is null.
   */
  public NettyNetworkClient(
    @NonNull EventManager eventManager,
    @NonNull ComponentInfo componentInfo,
    @NonNull Callable<NetworkChannelHandler> handlerFactory,
    @Nullable SSLConfiguration sslConfiguration
  ) {
    this.eventManager = eventManager;
    this.handlerFactory = handlerFactory;
    this.sslConfiguration = sslConfiguration;
    this.packetDispatcher = NettyUtil.newPacketDispatcher(componentInfo.environment());

    try {
      this.init();
    } catch (Exception exception) {
      LOGGER.severe("Exception while initializing the netty network client", exception);
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
  public @NonNull Task<Void> connect(@NonNull HostAndPort hostAndPort) {
    Task<Void> result = new Task<>();
    new Bootstrap()
      .group(this.eventLoopGroup)
      .channelFactory(NettyUtil.clientChannelFactory(hostAndPort.getProtocolFamily()))
      .handler(new NettyNetworkClientInitializer(hostAndPort, this.eventManager, this)
        .option(ChannelOption.IP_TOS, 0x18)
        .option(ChannelOption.AUTO_READ, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.TCP_FASTOPEN_CONNECT, true)
        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MILLIS))

      .connect(hostAndPort.toSocketAddress())
      .addListener(future -> {
        if (future.isSuccess()) {
          // ok, we connected successfully
          result.complete(null);
        } else {
          // something went wrong
          result.completeExceptionally(future.cause());
        }
      });

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.closeChannels();
    this.eventLoopGroup.shutdownGracefully();
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
  public @NonNull PacketListenerRegistry packetRegistry() {
    return this.packetRegistry;
  }

  /**
   * Builds the ssl context for this client if a ssl configuration was supplied and is active. If no certificates are
   * set in the given ssl configuration, the client will self-sign a certificate.
   *
   * @throws Exception if any exception occurs during the creation of the ssl context.
   */
  private void init() throws Exception {
    if (this.sslConfiguration != null && this.sslConfiguration.enabled()) {
      if (this.sslConfiguration.certificatePath() != null && this.sslConfiguration.privateKeyPath() != null) {
        // assign the trust certificate to the builder, trust all certificates if no trust certificate was specified
        var builder = SslContextBuilder.forClient();
        if (this.sslConfiguration.trustCertificatePath() != null) {
          try (var stream = Files.newInputStream(this.sslConfiguration.trustCertificatePath())) {
            builder.trustManager(stream);
          }
        } else {
          builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }

        // apply both certificates to the builder
        try (var cert = Files.newInputStream(this.sslConfiguration.certificatePath());
          var privateKey = Files.newInputStream(this.sslConfiguration.privateKeyPath())) {
          this.sslContext = builder
            .keyManager(cert, privateKey)
            .clientAuth(this.sslConfiguration.clientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL)
            .build();
        }
      } else {
        // self sign a certificate as non was given
        var selfSignedCertificate = new SelfSignedCertificate();
        this.sslContext = SslContextBuilder.forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .keyManager(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
          .build();
      }
    }
  }
}
