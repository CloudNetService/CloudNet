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

package eu.cloudnetservice.cloudnet.driver.network.netty.http;

import eu.cloudnetservice.cloudnet.common.collection.Pair;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpHandler;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpServer;
import eu.cloudnetservice.cloudnet.driver.network.netty.NettySslServer;
import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtils;
import eu.cloudnetservice.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of the web server, using netty as its backing mechanism.
 *
 * @since 4.0
 */
public class NettyHttpServer extends NettySslServer implements HttpServer {

  private static final Logger LOGGER = LogManager.logger(NettyHttpServer.class);

  protected final Collection<HttpHandlerEntry> registeredHandlers = new ConcurrentLinkedQueue<>();
  protected final Map<Integer, Pair<HostAndPort, ChannelFuture>> channelFutures = new ConcurrentHashMap<>();

  protected final EventLoopGroup bossGroup = NettyUtils.newEventLoopGroup();
  protected final EventLoopGroup workerGroup = NettyUtils.newEventLoopGroup();

  /**
   * Constructs a new instance of a netty http server instance. Equivalent to {@code new NettyHttpServer(null)}.
   */
  public NettyHttpServer() {
    this(null);
  }

  /**
   * Constructs a new netty http server instance with the given ssl configuration.
   *
   * @param sslConfiguration the ssl configuration to use, null for no ssl.
   */
  public NettyHttpServer(@Nullable SSLConfiguration sslConfiguration) {
    super(sslConfiguration);

    try {
      this.init();
    } catch (Exception exception) {
      LOGGER.severe("Exception initializing web server", exception);
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
  public boolean addListener(@NonNull SocketAddress socketAddress) {
    return this.addListener(HostAndPort.fromSocketAddress(socketAddress));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addListener(@NonNull HostAndPort hostAndPort) {
    try {
      if (!this.channelFutures.containsKey(hostAndPort.port())) {
        return this.channelFutures.putIfAbsent(hostAndPort.port(), new Pair<>(hostAndPort, new ServerBootstrap()
          .group(this.bossGroup, this.workerGroup)
          .channelFactory(NettyUtils.serverChannelFactory())
          .childHandler(new NettyHttpServerInitializer(this, hostAndPort))

          .childOption(ChannelOption.IP_TOS, 24)
          .childOption(ChannelOption.AUTO_READ, true)
          .childOption(ChannelOption.TCP_NODELAY, true)
          .childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)

          .bind(hostAndPort.host(), hostAndPort.port())
          .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
          .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)

          .sync()
          .channel()
          .closeFuture())) == null;
      }
    } catch (InterruptedException exception) {
      LOGGER.severe("Exception while binding http server to %s", exception, hostAndPort);
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer registerHandler(@NonNull String path, HttpHandler... handlers) {
    return this.registerHandler(path, HttpHandler.PRIORITY_NORMAL, handlers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer registerHandler(@NonNull String path, int priority, HttpHandler... handlers) {
    return this.registerHandler(path, null, priority, handlers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer registerHandler(
    @NonNull String path,
    @Nullable Integer port,
    int priority,
    @NonNull HttpHandler... handlers
  ) {
    // ensure that the path starts with a / (later needed for uri matching)
    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    // ensure that the path ends with a / (if the path is not the root handler)
    if (path.endsWith("/") && !path.equals("/")) {
      path = path.substring(0, path.length() - 1);
    }

    // register each handler
    register:
    for (var httpHandler : handlers) {
      // validate  that we are not registering a handler twice, except if the handler is another class
      for (var knownHandler : this.registeredHandlers) {
        if (knownHandler.path.equals(path) && knownHandler.httpHandler.getClass().equals(httpHandler.getClass())) {
          // a handler already exists - continue registering the provided handlers
          continue register;
        }
      }

      // register the handler
      this.registeredHandlers.add(new HttpHandlerEntry(path, httpHandler, port, priority));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer removeHandler(@NonNull HttpHandler handler) {
    this.registeredHandlers.removeIf(registeredHandler -> registeredHandler.httpHandler.equals(handler));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer removeHandler(@NonNull ClassLoader classLoader) {
    this.registeredHandlers.removeIf(handler -> handler.httpHandler.getClass().getClassLoader().equals(classLoader));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<HttpHandler> httpHandlers() {
    return this.registeredHandlers.stream()
      .map(HttpHandlerEntry::httpHandler)
      .toList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpServer clearHandlers() {
    this.registeredHandlers.clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    for (var entry : this.channelFutures.values()) {
      entry.second().cancel(true);
    }

    this.bossGroup.shutdownGracefully();
    this.workerGroup.shutdownGracefully();
    this.clearHandlers();
  }

  /**
   * Represents a registered http handler, holding all the information needed for later calling of it.
   *
   * @param path        the path to which the handler is bound.
   * @param httpHandler the actual registered handler to call.
   * @param port        the specific port the handlers listen to, or null if no port is selected.
   * @param priority    the priority of the handler.
   * @since 4.0
   */
  public record HttpHandlerEntry(
    @NonNull String path,
    @NonNull HttpHandler httpHandler,
    @Nullable Integer port,
    int priority
  ) implements Comparable<HttpHandlerEntry> {

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NonNull HttpHandlerEntry httpHandlerEntry) {
      return Integer.compare(this.priority, httpHandlerEntry.priority());
    }
  }
}
