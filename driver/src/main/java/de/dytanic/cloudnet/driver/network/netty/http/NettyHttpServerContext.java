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

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.http.HttpCookie;
import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import de.dytanic.cloudnet.driver.network.http.IHttpComponent;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import de.dytanic.cloudnet.driver.network.http.IHttpResponse;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
final class NettyHttpServerContext implements IHttpContext {

  private static final Logger LOGGER = LogManager.logger(NettyHttpServerContext.class);

  final NettyHttpServerResponse httpServerResponse;

  private final Channel nettyChannel;
  private final HttpRequest httpRequest;

  private final NettyHttpChannel channel;
  private final NettyHttpServer nettyHttpServer;
  private final NettyHttpServerRequest httpServerRequest;

  private final Collection<HttpCookie> cookies = new ArrayList<>();

  volatile boolean closeAfter = true;
  volatile boolean cancelNext = false;
  volatile boolean cancelSendResponse = false;

  private volatile String pathPrefix;
  private volatile IHttpHandler lastHandler;
  private volatile NettyWebSocketServerChannel webSocketServerChannel;

  public NettyHttpServerContext(
    @NonNull NettyHttpServer nettyHttpServer,
    @NonNull NettyHttpChannel channel,
    @NonNull URI uri,
    @NonNull Map<String, String> pathParameters,
    @NonNull HttpRequest httpRequest
  ) {
    this.nettyHttpServer = nettyHttpServer;
    this.channel = channel;
    this.httpRequest = httpRequest;
    this.nettyChannel = channel.channel();

    this.httpServerRequest = new NettyHttpServerRequest(this, httpRequest, pathParameters, uri);
    this.httpServerResponse = new NettyHttpServerResponse(this, httpRequest);

    if (this.httpRequest.headers().contains("Cookie")) {
      this.cookies.addAll(ServerCookieDecoder.LAX.decode(this.httpRequest.headers().get("Cookie")).stream()
        .map(cookie -> new HttpCookie(
          cookie.name(),
          cookie.value(),
          cookie.domain(),
          cookie.path(),
          cookie.isHttpOnly(),
          cookie.isSecure(),
          cookie.wrap(),
          cookie.maxAge()
        )).toList());
    }

    this.updateHeaderResponse();
  }

  @Override
  public @Nullable IWebSocketChannel upgrade() {
    if (this.webSocketServerChannel == null) {
      var handshaker = new WebSocketServerHandshakerFactory(
        this.httpRequest.uri(),
        null,
        true,
        Short.MAX_VALUE,
        false
      ).newHandshaker(this.httpRequest);
      if (handshaker == null) {
        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(this.nettyChannel);
        return null;
      } else {
        this.nettyChannel.pipeline().remove("http-server-handler");
        try {
          handshaker.handshake(this.nettyChannel, this.httpRequest);
        } catch (WebSocketHandshakeException exception) {
          this.nettyChannel.writeAndFlush(new DefaultFullHttpResponse(
            this.httpRequest.protocolVersion(),
            HttpResponseStatus.OK,
            Unpooled.wrappedBuffer("Unable to upgrade connection".getBytes())
          ));
          LOGGER.severe("Exception during websocket handshake", exception);
          return null;
        }
      }

      this.webSocketServerChannel = new NettyWebSocketServerChannel(this.channel, this.nettyChannel);
      this.nettyChannel.pipeline().addLast("websocket-server-channel-handler",
        new NettyWebSocketServerChannelHandler(this.webSocketServerChannel));

      this.cancelNext(true);
      this.closeAfter(false);
      this.cancelSendResponse = true;
    }

    return this.webSocketServerChannel;
  }

  @Override
  public @Nullable IWebSocketChannel webSocketChanel() {
    return this.webSocketServerChannel;
  }

  @Override
  public @NonNull IHttpChannel channel() {
    return this.channel;
  }

  @Override
  public @NonNull IHttpRequest request() {
    return this.httpServerRequest;
  }

  @Override
  public @NonNull IHttpResponse response() {
    return this.httpServerResponse;
  }

  @Override
  public boolean cancelNext() {
    return this.cancelNext = true;
  }

  @Override
  public @NonNull IHttpContext cancelNext(boolean cancelNext) {
    this.cancelNext = cancelNext;
    return this;
  }

  @Override
  public @Nullable IHttpHandler peekLast() {
    return this.lastHandler;
  }

  @Override
  public @NonNull IHttpComponent<IHttpServer> component() {
    return this.nettyHttpServer;
  }

  @Override
  public @NonNull IHttpContext closeAfter(boolean value) {
    this.closeAfter = value;
    return this;
  }

  @Override
  public boolean closeAfter() {
    return this.closeAfter;
  }

  @Override
  public HttpCookie cookie(@NonNull String name) {
    return this.cookies.stream()
      .filter(httpCookie -> httpCookie.name().equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);
  }

  @Override
  public @NonNull Collection<HttpCookie> cookies() {
    return this.cookies;
  }

  @Override
  public boolean hasCookie(@NonNull String name) {
    return this.cookies.stream().anyMatch(httpCookie -> httpCookie.name().equalsIgnoreCase(name));
  }

  @Override
  public @NonNull IHttpContext cookies(@NonNull Collection<HttpCookie> cookies) {
    this.cookies.clear();
    this.cookies.addAll(cookies);

    this.updateHeaderResponse();
    return this;
  }

  @Override
  public @NonNull IHttpContext addCookie(@NonNull HttpCookie httpCookie) {
    var cookie = this.cookie(httpCookie.name());
    if (cookie != null) {
      this.removeCookie(cookie.name());
    }

    this.cookies.add(httpCookie);
    this.updateHeaderResponse();

    return this;
  }

  @Override
  public @NonNull IHttpContext removeCookie(@NonNull String name) {
    this.cookies.removeIf(cookie -> cookie.name().equals(name));
    this.updateHeaderResponse();
    return this;
  }

  @Override
  public @NonNull IHttpContext clearCookies() {
    this.cookies.clear();
    this.updateHeaderResponse();
    return this;
  }

  @Override
  public @NonNull String pathPrefix() {
    return this.pathPrefix;
  }

  public void pathPrefix(@NonNull String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  private void updateHeaderResponse() {
    if (this.cookies.isEmpty()) {
      this.httpServerResponse.httpResponse.headers().remove("Set-Cookie");
    } else {
      this.httpServerResponse.httpResponse.headers().set("Set-Cookie",
        ServerCookieEncoder.LAX.encode(this.cookies.stream().map(httpCookie -> {
          Cookie cookie = new DefaultCookie(httpCookie.name(), httpCookie.value());
          cookie.setDomain(httpCookie.domain());
          cookie.setMaxAge(httpCookie.maxAge());
          cookie.setPath(httpCookie.path());
          cookie.setSecure(httpCookie.secure());
          cookie.setHttpOnly(httpCookie.httpOnly());
          cookie.setWrap(httpCookie.wrap());

          return cookie;
        }).collect(Collectors.toList())));
    }
  }

  public void setLastHandler(@NonNull IHttpHandler lastHandler) {
    this.lastHandler = lastHandler;
  }
}
