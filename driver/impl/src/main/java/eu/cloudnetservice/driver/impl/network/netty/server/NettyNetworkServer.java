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

package eu.cloudnetservice.driver.impl.network.netty.server;

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.impl.network.netty.NettyOptionSettingChannelInitializer;
import eu.cloudnetservice.driver.impl.network.netty.NettyUtil;
import eu.cloudnetservice.driver.impl.network.protocol.DefaultPacketListenerRegistry;
import eu.cloudnetservice.driver.impl.network.scheduler.NetworkTaskScheduler;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.WriteBufferWaterMark;
import io.netty5.channel.unix.UnixChannelOption;
import io.netty5.handler.ssl.ClientAuth;
import io.netty5.handler.ssl.IdentityCipherSuiteFilter;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import io.netty5.util.concurrent.Future;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty based implementation of the network server.
 *
 * @since 4.0
 */
@Singleton
public class NettyNetworkServer implements NetworkServer {

  protected static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(1 << 20, 1 << 21);

  protected final SslContext sslContext;
  protected final EventLoopGroup bossEventLoopGroup;
  protected final EventLoopGroup workerEventLoopGroup;

  protected final Collection<NetworkChannel> channels = ConcurrentHashMap.newKeySet();
  protected final Map<HostAndPort, Future<Void>> channelFutures = new ConcurrentHashMap<>();

  protected final PacketListenerRegistry packetRegistry = new DefaultPacketListenerRegistry();

  protected final NetworkTaskScheduler packetDispatcher;
  protected final Callable<NetworkChannelHandler> handlerFactory;

  /**
   * Constructs a new netty network server instance. Equivalent to {@code new NettyNetworkServer(factory, null)}.
   *
   * @param componentInfo  the component info of the current component the server is created for.
   * @param handlerFactory the handler factory to use.
   * @throws NullPointerException if the given event manager, component info or factory is null.
   */
  public NettyNetworkServer(
    @NonNull ComponentInfo componentInfo,
    @NonNull Callable<NetworkChannelHandler> handlerFactory
  ) {
    this(componentInfo, handlerFactory, null);
  }

  /**
   * Constructs a new netty network server instance.
   *
   * @param componentInfo    the component info of the current component the server is created for.
   * @param handlerFactory   the handler factory to use.
   * @param sslConfiguration the ssl configuration to apply, or null if ssl should be disabled.
   * @throws NullPointerException if the given event manager, component info or factory is null.
   */
  public NettyNetworkServer(
    @NonNull ComponentInfo componentInfo,
    @NonNull Callable<NetworkChannelHandler> handlerFactory,
    @Nullable SSLConfiguration sslConfiguration
  ) {
    this.handlerFactory = handlerFactory;

    this.sslContext = initializeSslContext(sslConfiguration);
    this.bossEventLoopGroup = NettyUtil.createBossEventLoopGroup();
    this.workerEventLoopGroup = NettyUtil.createWorkerEventLoopGroup(componentInfo.environment());

    this.packetDispatcher = NettyUtil.createPacketDispatcher(componentInfo.environment());
  }

  /**
   * Builds the ssl context for this server if a ssl configuration was supplied and is active.
   *
   * @param sslConfig the supplied ssl configuration to build the ssl context based on, can be null.
   * @return the constructed ssl context based on the given configuration.
   * @throws IllegalStateException    if an error occurs during construction of the ssl context.
   * @throws IllegalArgumentException if the trust certificate chain or private key file is invalid.
   */
  private static @Nullable SslContext initializeSslContext(@Nullable SSLConfiguration sslConfig) {
    if (sslConfig == null || !sslConfig.enabled()) {
      return null;
    }

    // if client auth is enabled the server will require authorization from the server,
    // in all other cases we can just skip that step
    var clientAuthMode = sslConfig.clientAuth() ? ClientAuth.REQUIRE : ClientAuth.NONE;

    try {
      var keyPath = sslConfig.privateKeyPath();
      var keyCertificatePath = sslConfig.certificatePath();
      if (keyPath != null
        && keyCertificatePath != null
        && Files.isRegularFile(keyPath)
        && Files.isRegularFile(keyCertificatePath)) {
        // use the keys provided in the ssl configuration
        try (
          var keyStream = Files.newInputStream(keyPath, StandardOpenOption.READ);
          var keyCertChainStream = Files.newInputStream(keyCertificatePath, StandardOpenOption.READ)
        ) {
          return SslContextBuilder.forServer(keyCertChainStream, keyStream, sslConfig.privateKeyPassword())
            .clientAuth(clientAuthMode)
            .applicationProtocolConfig(null)
            .sslProvider(NettyUtil.selectedSslProvider())
            .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
            .build();
        }
      } else {
        // provided paths are not given or invalid, use a self-signed certificate instead
        var selfSignedCertificate = new SelfSignedCertificate();
        return SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
          .clientAuth(clientAuthMode)
          .applicationProtocolConfig(null)
          .sslProvider(NettyUtil.selectedSslProvider())
          .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
          .build();
      }
    } catch (SSLException exception) {
      var errorMessage = String.format("Unable to build server ssl provider from configuration %s", sslConfig);
      throw new IllegalStateException(errorMessage, exception);
    } catch (CertificateException exception) {
      var errorMessage = String.format("Unable to generated self-signed certificate; ssl-config: %s", sslConfig);
      throw new IllegalStateException(errorMessage, exception);
    } catch (IOException exception) {
      var errorMessage = String.format("Unable to open cert chain or private key for server ssl config %s", sslConfig);
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
  public @NonNull CompletableFuture<Void> addListener(int port) {
    return this.addListener(new HostAndPort("0.0.0.0", port));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Void> addListener(@NonNull HostAndPort hostAndPort) {
    CompletableFuture<Void> result = new CompletableFuture<>();
    new ServerBootstrap()
      .channelFactory(NettyUtil.serverChannelFactory())
      .group(this.bossEventLoopGroup, this.workerEventLoopGroup)

      .handler(new NettyOptionSettingChannelInitializer()
        .option(ChannelOption.TCP_FASTOPEN, 3)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(UnixChannelOption.SO_REUSEPORT, true)
        .option(ChannelOption.BUFFER_ALLOCATOR, NettyUtil.selectedBufferAllocator()))
      .childHandler(new NettyNetworkServerInitializer(this, hostAndPort)
        .option(ChannelOption.IP_TOS, 0x18)
        .option(ChannelOption.AUTO_READ, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK)
        .option(ChannelOption.BUFFER_ALLOCATOR, NettyUtil.selectedBufferAllocator()))

      .bind(hostAndPort.host(), hostAndPort.port())
      .addListener(future -> {
        if (future.isSuccess()) {
          result.complete(null);
          this.channelFutures.put(hostAndPort, future.getNow().closeFuture());
        } else {
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
    for (var entry : this.channelFutures.values()) {
      entry.cancel();
    }

    this.packetDispatcher.shutdown();
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
