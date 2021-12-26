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

package de.dytanic.cloudnet.driver.network.netty.http;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.HttpHandler;
import de.dytanic.cloudnet.driver.network.http.HttpServer;
import de.dytanic.cloudnet.driver.network.netty.NettySSLServer;
import de.dytanic.cloudnet.driver.network.netty.NettyUtils;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class NettyHttpServer extends NettySSLServer implements HttpServer {

  private static final Logger LOGGER = LogManager.logger(NettyHttpServer.class);

  protected final List<HttpHandlerEntry> registeredHandlers = new CopyOnWriteArrayList<>();
  protected final Map<Integer, Pair<HostAndPort, ChannelFuture>> channelFutures = new ConcurrentHashMap<>();

  protected final EventLoopGroup bossGroup = NettyUtils.newEventLoopGroup();
  protected final EventLoopGroup workerGroup = NettyUtils.newEventLoopGroup();

  public NettyHttpServer() {
    this(null);
  }

  public NettyHttpServer(SSLConfiguration sslConfiguration) {
    super(sslConfiguration);

    try {
      this.init();
    } catch (Exception exception) {
      LOGGER.severe("Exception initializing web server", exception);
    }
  }

  @Override
  public boolean sslEnabled() {
    return this.sslContext != null;
  }

  @Override
  public boolean addListener(int port) {
    return this.addListener(new HostAndPort("0.0.0.0", port));
  }

  @Override
  public boolean addListener(@NonNull SocketAddress socketAddress) {
    return this.addListener(HostAndPort.fromSocketAddress(socketAddress));
  }

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

  @Override
  public @NonNull HttpServer registerHandler(@NonNull String path, HttpHandler... handlers) {
    return this.registerHandler(path, HttpHandler.PRIORITY_NORMAL, handlers);
  }

  @Override
  public @NonNull HttpServer registerHandler(@NonNull String path, int priority, HttpHandler... handlers) {
    return this.registerHandler(path, null, priority, handlers);
  }

  @Override
  public @NonNull HttpServer registerHandler(
    @NonNull String path, Integer port,
    int priority,
    HttpHandler... handlers
  ) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    if (path.endsWith("/") && !path.equals("/")) {
      path = path.substring(0, path.length() - 1);
    }

    for (var httpHandler : handlers) {
      if (httpHandler != null) {
        var value = true;

        for (var registeredHandler : this.registeredHandlers) {
          if (registeredHandler.path.equals(path) && registeredHandler.httpHandler.getClass()
            .equals(httpHandler.getClass())) {
            value = false;
            break;
          }
        }

        if (value) {
          this.registeredHandlers.add(new HttpHandlerEntry(path, httpHandler, port, priority));
        }
      }
    }

    return this;
  }

  @Override
  public @NonNull HttpServer removeHandler(@NonNull HttpHandler handler) {
    this.registeredHandlers.removeIf(registeredHandler -> registeredHandler.httpHandler.equals(handler));
    return this;
  }

  @Override
  public @NonNull HttpServer removeHandler(@NonNull Class<? extends HttpHandler> handler) {
    this.registeredHandlers.removeIf(registeredHandler -> registeredHandler.httpHandler.getClass().equals(handler));
    return this;
  }

  @Override
  public @NonNull HttpServer removeHandler(@NonNull ClassLoader classLoader) {
    this.registeredHandlers.removeIf(handler -> handler.httpHandler.getClass().getClassLoader().equals(classLoader));
    return this;
  }

  @Override
  public @NonNull Collection<HttpHandler> httpHandlers() {
    return this.registeredHandlers.stream()
      .map(httpHandlerEntry -> httpHandlerEntry.httpHandler)
      .toList();
  }

  @Override
  public @NonNull HttpServer clearHandlers() {
    this.registeredHandlers.clear();
    return this;
  }

  @Override
  public void close() {
    for (var entry : this.channelFutures.values()) {
      entry.second().cancel(true);
    }

    this.bossGroup.shutdownGracefully();
    this.workerGroup.shutdownGracefully();
    this.clearHandlers();
  }

  public record HttpHandlerEntry(
    @NonNull String path,
    @NonNull HttpHandler httpHandler,
    @Nullable Integer port,
    int priority
  ) implements Comparable<HttpHandlerEntry> {

    @Override
    public int compareTo(@NonNull HttpHandlerEntry httpHandlerEntry) {
      return this.priority + httpHandlerEntry.priority;
    }
  }
}
