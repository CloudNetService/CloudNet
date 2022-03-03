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

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpChannel;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpComponent;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpCookie;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpHandler;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpRequest;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponse;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpServer;
import eu.cloudnetservice.cloudnet.driver.network.http.websocket.WebSocketChannel;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty implementation of a http handling context.
 *
 * @since 4.0
 */
@Internal
final class NettyHttpServerContext implements HttpContext {

  private static final Logger LOGGER = LogManager.logger(NettyHttpServerContext.class);

  final NettyHttpServerResponse httpServerResponse;

  private final Channel nettyChannel;
  private final io.netty.handler.codec.http.HttpRequest httpRequest;

  private final NettyHttpChannel channel;
  private final NettyHttpServer nettyHttpServer;
  private final NettyHttpServerRequest httpServerRequest;

  private final Collection<HttpCookie> cookies = new ArrayList<>();

  volatile boolean closeAfter = true;
  volatile boolean cancelNext = false;
  volatile boolean cancelSendResponse = false;

  private volatile String pathPrefix;
  private volatile HttpHandler lastHandler;
  private volatile NettyWebSocketServerChannel webSocketServerChannel;

  /**
   * Constructs a new netty http server context instance.
   *
   * @param nettyHttpServer the http server which received the request handled by this context.
   * @param channel         the channel to which the request was sent.
   * @param uri             the uri of the request.
   * @param pathParameters  the path parameters pre-parsed, by default an empty map.
   * @param httpRequest     the http request which was received originally.
   * @throws NullPointerException if one of the constructor parameters is null.
   */
  public NettyHttpServerContext(
    @NonNull NettyHttpServer nettyHttpServer,
    @NonNull NettyHttpChannel channel,
    @NonNull URI uri,
    @NonNull Map<String, String> pathParameters,
    @NonNull io.netty.handler.codec.http.HttpRequest httpRequest
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

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable WebSocketChannel upgrade() {
    if (this.webSocketServerChannel == null) {
      // not upgraded yet, build a new handshaker based on the given information
      var handshaker = new WebSocketServerHandshakerFactory(
        this.httpRequest.uri(),
        null,
        true,
        Short.MAX_VALUE,
        false
      ).newHandshaker(this.httpRequest);
      // no handshaker (as per the netty docs) means that the websocket version of the request is unsupported.
      if (handshaker == null) {
        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(this.nettyChannel);
        return null;
      } else {
        // remove the http handler from the pipeline, gets replaced with the websocket one
        this.nettyChannel.pipeline().remove("http-server-handler");
        this.nettyChannel.pipeline().remove("read-timeout-handler");

        // try to greet the client, block until the operation is done
        try {
          handshaker.handshake(this.nettyChannel, this.httpRequest).syncUninterruptibly();
        } catch (Exception exception) {
          this.nettyChannel.writeAndFlush(new DefaultFullHttpResponse(
            this.httpRequest.protocolVersion(),
            HttpResponseStatus.OK,
            Unpooled.wrappedBuffer("Unable to upgrade connection".getBytes())
          ));
          LOGGER.severe("Exception during websocket handshake", exception);
          return null;
        }
      }

      // upgrading done, add the handler to the pipeline
      this.webSocketServerChannel = new NettyWebSocketServerChannel(this.channel, this.nettyChannel);
      this.nettyChannel.pipeline().addLast(
        "websocket-server-channel-handler",
        new NettyWebSocketServerChannelHandler(this.webSocketServerChannel));

      // cancel the next request and the response send to the client by the handler
      this.cancelNext(true);
      this.closeAfter(false);
      this.cancelSendResponse = true;
    }

    return this.webSocketServerChannel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable WebSocketChannel webSocketChanel() {
    return this.webSocketServerChannel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpChannel channel() {
    return this.channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpRequest request() {
    return this.httpServerRequest;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpResponse response() {
    return this.httpServerResponse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancelNext() {
    return this.cancelNext = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext cancelNext(boolean cancelNext) {
    this.cancelNext = cancelNext;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable HttpHandler peekLast() {
    return this.lastHandler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpComponent<HttpServer> component() {
    return this.nettyHttpServer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext closeAfter(boolean value) {
    this.closeAfter = value;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean closeAfter() {
    return this.closeAfter = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpCookie cookie(@NonNull String name) {
    return this.cookies.stream()
      .filter(httpCookie -> httpCookie.name().equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<HttpCookie> cookies() {
    return this.cookies;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasCookie(@NonNull String name) {
    return this.cookies.stream().anyMatch(httpCookie -> httpCookie.name().equalsIgnoreCase(name));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext cookies(@NonNull Collection<HttpCookie> cookies) {
    this.cookies.clear();
    this.cookies.addAll(cookies);

    this.updateHeaderResponse();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext addCookie(@NonNull HttpCookie httpCookie) {
    var cookie = this.cookie(httpCookie.name());
    if (cookie != null) {
      this.removeCookie(cookie.name());
    }

    this.cookies.add(httpCookie);
    this.updateHeaderResponse();

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext removeCookie(@NonNull String name) {
    this.cookies.removeIf(cookie -> cookie.name().equals(name));
    this.updateHeaderResponse();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext clearCookies() {
    this.cookies.clear();
    this.updateHeaderResponse();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String pathPrefix() {
    return this.pathPrefix;
  }

  /**
   * Sets the current path prefix of the handler being processed.
   *
   * @param pathPrefix the path prefix of the current handler.
   * @throws NullPointerException if the given path prefix is null.
   */
  public void pathPrefix(@NonNull String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  /**
   * Sets the last handler which was processed in the processing chain, for later access from the next handler in the
   * chain. Use {@link #peekLast()} to get the previous handler in the chain.
   *
   * @param lastHandler the last processed handler in the chain.
   * @throws NullPointerException if the last handler is null.
   */
  public void pushChain(@NonNull HttpHandler lastHandler) {
    this.lastHandler = lastHandler;
  }

  /**
   * Updates the response header according to the new cookies set during the request.
   */
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
}
