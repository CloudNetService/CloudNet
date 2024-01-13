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

package eu.cloudnetservice.driver.network.netty.http;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.http.HttpChannel;
import eu.cloudnetservice.driver.network.http.HttpComponent;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpCookie;
import eu.cloudnetservice.driver.network.http.HttpHandler;
import eu.cloudnetservice.driver.network.http.HttpRequest;
import eu.cloudnetservice.driver.network.http.HttpResponse;
import eu.cloudnetservice.driver.network.http.HttpServer;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketChannel;
import io.netty5.buffer.DefaultBufferAllocators;
import io.netty5.channel.Channel;
import io.netty5.handler.codec.http.DefaultFullHttpResponse;
import io.netty5.handler.codec.http.HttpResponseStatus;
import io.netty5.handler.codec.http.headers.DefaultHttpSetCookie;
import io.netty5.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty implementation of a http handling context.
 *
 * @since 4.0
 */
final class NettyHttpServerContext implements HttpContext {

  final NettyHttpServerResponse httpServerResponse;
  final Multimap<String, Object> invocationHints = ArrayListMultimap.create();

  private final Channel nettyChannel;
  private final io.netty5.handler.codec.http.HttpRequest httpRequest;

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
    @NonNull io.netty5.handler.codec.http.HttpRequest httpRequest
  ) {
    this.nettyHttpServer = nettyHttpServer;
    this.channel = channel;
    this.httpRequest = httpRequest;
    this.nettyChannel = channel.channel();

    this.httpServerRequest = new NettyHttpServerRequest(this, httpRequest, pathParameters, uri);
    this.httpServerResponse = new NettyHttpServerResponse(this, httpRequest);

    var cookiesIterator = this.httpRequest.headers().getCookiesIterator();
    while (cookiesIterator.hasNext()) {
      var cookie = cookiesIterator.next();
      var httpCookie = new HttpCookie(
        cookie.name().toString(),
        cookie.value().toString(),
        null,
        null,
        false,
        false,
        cookie.isWrapped(),
        0);
      this.cookies.add(httpCookie);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Task<WebSocketChannel> upgrade() {
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
      // in both cases we don't want to respond - in case there is no handshaker we're sending out a response here,
      // in case we upgraded there is no need to send a response
      this.cancelSendResponse = true;
      if (handshaker == null) {
        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(this.nettyChannel);
        return Task.completedTask(new IllegalStateException("Unsupported web socket version"));
      } else {
        // remove the http handler from the pipeline, gets replaced with the websocket one
        this.nettyChannel.pipeline().remove("http-server-handler");
        this.nettyChannel.pipeline().remove("read-timeout-handler");

        // cancel the next request and the response send to the client by the handler to do our processing
        this.cancelNext(true);
        this.closeAfter(false);

        // try to greet the client
        Task<WebSocketChannel> task = new Task<>();
        handshaker.handshake(this.nettyChannel, this.httpRequest).addListener(future -> {
          if (future.isSuccess()) {
            // successfully greeted the client, setup everything we need
            this.webSocketServerChannel = new NettyWebSocketServerChannel(this.channel, this.nettyChannel);
            this.nettyChannel.pipeline().addLast(
              "websocket-server-channel-handler",
              new NettyWebSocketServerChannelHandler(this.webSocketServerChannel));

            // done :)
            task.complete(this.webSocketServerChannel);
          } else {
            // something went wrong...
            this.nettyChannel.writeAndFlush(new DefaultFullHttpResponse(
              this.httpRequest.protocolVersion(),
              HttpResponseStatus.OK,
              DefaultBufferAllocators.offHeapAllocator().copyOf("Unable to upgrade connection".getBytes())
            ));
            task.completeExceptionally(future.cause());
          }
        });
        return task;
      }
    } else {
      return Task.completedTask(this.webSocketServerChannel);
    }
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
    cookies.forEach(this::addCookie);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext addCookie(@NonNull HttpCookie httpCookie) {
    var cookie = new DefaultHttpSetCookie(
      httpCookie.name(),
      httpCookie.value(),
      httpCookie.path(),
      httpCookie.domain(),
      null,
      httpCookie.maxAge(),
      null,
      httpCookie.wrap(),
      httpCookie.secure(),
      httpCookie.httpOnly());
    this.httpServerResponse.httpResponse.headers().addSetCookie(cookie);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext removeCookie(@NonNull String name) {
    this.httpServerResponse.httpResponse.headers().removeSetCookies(name);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext clearCookies() {
    var cookieIterator = this.httpServerResponse.httpResponse.headers().getSetCookiesIterator();
    while (cookieIterator.hasNext()) {
      cookieIterator.remove();
    }
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
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Object> invocationHints(@NonNull String key) {
    return this.invocationHints.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpContext addInvocationHint(@NonNull String key, @NonNull Object value) {
    this.invocationHints.put(key, value);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> HttpContext addInvocationHints(@NonNull String key, @NonNull Collection<T> value) {
    this.invocationHints.putAll(key, value);
    return this;
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
}
