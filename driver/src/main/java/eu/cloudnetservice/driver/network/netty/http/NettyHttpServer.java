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

package eu.cloudnetservice.driver.network.netty.http;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.network.http.HttpHandler;
import eu.cloudnetservice.driver.network.http.HttpServer;
import eu.cloudnetservice.driver.network.http.annotation.parser.DefaultHttpAnnotationParser;
import eu.cloudnetservice.driver.network.http.annotation.parser.HttpAnnotationParser;
import eu.cloudnetservice.driver.network.netty.NettyOptionSettingChannelInitializer;
import eu.cloudnetservice.driver.network.netty.NettySslServer;
import eu.cloudnetservice.driver.network.netty.NettyUtil;
import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.util.concurrent.Future;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of the web server, using netty as its backing mechanism.
 *
 * @since 4.0
 */
@Singleton
public class NettyHttpServer extends NettySslServer implements HttpServer {

  private static final Logger LOGGER = LogManager.logger(NettyHttpServer.class);

  protected final Map<HostAndPort, Future<Void>> channelFutures = new ConcurrentHashMap<>();
  protected final Collection<HttpHandlerEntry> registeredHandlers = ConcurrentHashMap.newKeySet();

  protected final EventLoopGroup bossGroup = NettyUtil.newEventLoopGroup(1);
  protected final EventLoopGroup workerGroup = NettyUtil.newEventLoopGroup(0);

  protected final HttpAnnotationParser<HttpServer> annoParser = DefaultHttpAnnotationParser.withDefaultProcessors(this);

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
  public @NonNull HttpAnnotationParser<HttpServer> annotationParser() {
    return this.annoParser;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<Void> addListener(int port) {
    return this.addListener(new HostAndPort("0.0.0.0", port));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<Void> addListener(@NonNull HostAndPort hostAndPort) {
    Task<Void> result = new Task<>();
    new ServerBootstrap()
      .group(this.bossGroup, this.workerGroup)
      .channelFactory(NettyUtil.serverChannelFactory(hostAndPort.getProtocolFamily()))
      .handler(new NettyOptionSettingChannelInitializer()
        .option(ChannelOption.SO_REUSEADDR, true))
      .childHandler(new NettyHttpServerInitializer(this, hostAndPort)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true))
      
      .childOption(ChannelOption.AUTO_READ, true)

      .bind(hostAndPort.toSocketAddress())
      .addListener(future -> {
        if (future.isSuccess()) {
          // ok, we bound successfully
          result.complete(null);
          this.channelFutures.put(hostAndPort, future.getNow().closeFuture());
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
    for (var httpHandler : handlers) {
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
      entry.cancel();
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
