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

package eu.cloudnetservice.driver.impl.network.netty.client;

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.impl.network.netty.NettyUtil;
import eu.cloudnetservice.driver.impl.network.protocol.DefaultPacketListenerRegistry;
import eu.cloudnetservice.driver.impl.network.scheduler.NetworkTaskScheduler;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.WriteBufferWaterMark;
import io.netty5.handler.ssl.IdentityCipherSuiteFilter;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a network client, using the netty network client implementations.
 *
 * @since 4.0
 */
@Singleton
public class NettyNetworkClient implements NetworkClient {

  private static final int CONNECTION_TIMEOUT_MILLIS = 5_000;
  private static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(1 << 20, 1 << 21);

  protected final SslContext sslContext;
  protected final EventLoopGroup eventLoopGroup;

  protected final Collection<NetworkChannel> channels = ConcurrentHashMap.newKeySet();
  protected final PacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

  protected final NetworkTaskScheduler packetDispatcher;
  protected final Callable<NetworkChannelHandler> handlerFactory;

  /**
   * Constructs a new netty network client instance. Equivalent to {@code new NettyNetworkClient(handlerFactory, null)}
   *
   * @param componentInfo  the component info of the current component the client is created for.
   * @param handlerFactory the factory for new handlers to be created with.
   * @throws NullPointerException if the given event manager, component info or factory is null.
   */
  public NettyNetworkClient(
    @NonNull ComponentInfo componentInfo,
    @NonNull Callable<NetworkChannelHandler> handlerFactory
  ) {
    this(componentInfo, handlerFactory, null);
  }

  /**
   * Constructs a new netty network client instance.
   *
   * @param componentInfo    the component info of the current component the client is created for.
   * @param handlerFactory   the factory for new handler to be created with.
   * @param sslConfiguration the ssl configuration applying to this client, or null for no ssl configuration.
   * @throws NullPointerException if the given event manager, component info or factory is null.
   */
  public NettyNetworkClient(
    @NonNull ComponentInfo componentInfo,
    @NonNull Callable<NetworkChannelHandler> handlerFactory,
    @Nullable SSLConfiguration sslConfiguration
  ) {
    this.handlerFactory = handlerFactory;

    this.sslContext = initializeSslContext(sslConfiguration);
    this.packetDispatcher = NettyUtil.createPacketDispatcher(componentInfo.environment());
    this.eventLoopGroup = NettyUtil.createWorkerEventLoopGroup(componentInfo.environment());
  }

  /**
   * Builds the ssl context for this client if a ssl configuration was supplied and is active.
   *
   * @param sslConfig the supplied ssl configuration to build the ssl context based on, can be null.
   * @return the constructed ssl context based on the given configuration.
   * @throws IllegalStateException    if an error occurs during construction of the ssl context.
   * @throws IllegalArgumentException if the trust certificate file does not contain any valid certificates.
   */
  private static @Nullable SslContext initializeSslContext(@Nullable SSLConfiguration sslConfig) {
    if (sslConfig == null || !sslConfig.enabled()) {
      return null;
    }

    try {
      var trustCertificatePath = sslConfig.trustCertificatePath();
      if (trustCertificatePath != null && Files.isRegularFile(trustCertificatePath)) {
        // trust certificate path exists, load the trust certificate from that path
        try (var trustCertCollectionStream = Files.newInputStream(trustCertificatePath, StandardOpenOption.READ)) {
          return SslContextBuilder.forClient()
            .applicationProtocolConfig(null)
            .trustManager(trustCertCollectionStream)
            .sslProvider(NettyUtil.selectedSslProvider())
            .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
            .build();
        }
      } else {
        // no trust certificate path provided or file does not exist, fall back
        // to just accepting all server certificates
        return SslContextBuilder.forClient()
          .applicationProtocolConfig(null)
          .sslProvider(NettyUtil.selectedSslProvider())
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
          .build();
      }
    } catch (SSLException exception) {
      var errorMessage = String.format("Unable to build client ssl provider from configuration %s", sslConfig);
      throw new IllegalStateException(errorMessage, exception);
    } catch (IOException exception) {
      var errorMessage = String.format(
        "Unable to open trust certificate at %s for reading",
        sslConfig.trustCertificatePath());
      throw new IllegalStateException(errorMessage, exception);
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
  public @NonNull CompletableFuture<Void> connect(@NonNull HostAndPort hostAndPort) {
    CompletableFuture<Void> result = new CompletableFuture<>();
    new Bootstrap()
      .group(this.eventLoopGroup)
      .channelFactory(NettyUtil.clientChannelFactory())
      .handler(new NettyNetworkClientInitializer(hostAndPort, this)
        .option(ChannelOption.IP_TOS, 0x18)
        .option(ChannelOption.AUTO_READ, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.TCP_FASTOPEN_CONNECT, true)
        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MILLIS)
        .option(ChannelOption.BUFFER_ALLOCATOR, NettyUtil.selectedBufferAllocator()))

      .connect(hostAndPort.host(), hostAndPort.port())
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
    this.packetDispatcher.shutdown();
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
  public @NonNull PacketListenerRegistry packetRegistry() {
    return this.packetRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacket(@NonNull Packet packet) {
    for (var channel : this.channels) {
      channel.sendPacket(packet);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendPacketSync(@NonNull Packet packet) {
    for (var channel : this.channels) {
      channel.sendPacketSync(packet);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void closeChannels() {
    var iterator = this.channels.iterator();
    while (iterator.hasNext()) {
      iterator.next().close();
      iterator.remove();
    }
  }
}
