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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.http.HttpCookie;
import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import de.dytanic.cloudnet.driver.network.http.IHttpComponent;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import de.dytanic.cloudnet.driver.network.http.IHttpResponse;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyHttpServerContext implements IHttpContext {

  protected final Collection<HttpCookie> cookies = new ArrayList<>();

  protected final Channel nettyChannel;

  protected final NettyHttpChannel channel;
  protected final NettyHttpServer nettyHttpServer;

  protected final HttpRequest httpRequest;

  protected final NettyHttpServerRequest httpServerRequest;
  protected final NettyHttpServerResponse httpServerResponse;

  protected volatile boolean closeAfter = true;
  protected volatile boolean cancelNext = false;
  protected volatile boolean cancelSendResponse = false;

  protected volatile NettyWebSocketServerChannel webSocketServerChannel;

  protected IHttpHandler lastHandler;

  public NettyHttpServerContext(NettyHttpServer nettyHttpServer, NettyHttpChannel channel, URI uri,
    Map<String, String> pathParameters, HttpRequest httpRequest) {
    this.nettyHttpServer = nettyHttpServer;
    this.channel = channel;
    this.httpRequest = httpRequest;
    this.nettyChannel = channel.getChannel();

    this.httpServerRequest = new NettyHttpServerRequest(this, httpRequest, pathParameters, uri);
    this.httpServerResponse = new NettyHttpServerResponse(this, httpRequest);

    if (this.httpRequest.headers().contains("Cookie")) {
      this.cookies.addAll(
        ServerCookieDecoder.LAX.decode(this.httpRequest.headers().get("Cookie")).stream().map(cookie -> new HttpCookie(
          cookie.name(),
          cookie.value(),
          cookie.domain(),
          cookie.path(),
          cookie.maxAge()
        )).collect(Collectors.toList()));
    }

    this.updateHeaderResponse();
  }

  @Override
  public IWebSocketChannel upgrade() {
    if (this.webSocketServerChannel == null) {
      this.cancelSendResponse = true;
      WebSocketServerHandshakerFactory webSocketServerHandshakerFactory = new WebSocketServerHandshakerFactory(
        this.httpRequest.uri(),
        null,
        false
      );

      this.nettyChannel.pipeline().remove("http-server-handler");

      WebSocketServerHandshaker webSocketServerHandshaker = webSocketServerHandshakerFactory
        .newHandshaker(this.httpRequest);
      webSocketServerHandshaker.handshake(this.nettyChannel, this.httpRequest);

      this.webSocketServerChannel = new NettyWebSocketServerChannel(this.channel, this.nettyChannel,
        webSocketServerHandshaker);
      this.nettyChannel.pipeline().addLast("websocket-server-channel-handler",
        new NettyWebSocketServerChannelHandler(this.webSocketServerChannel));

      this.closeAfter(false);
    }

    return this.webSocketServerChannel;
  }

  @Override
  public IWebSocketChannel webSocketChanel() {
    return this.webSocketServerChannel;
  }

  @Override
  public IHttpChannel channel() {
    return this.channel;
  }

  @Override
  public IHttpRequest request() {
    return this.httpServerRequest;
  }

  @Override
  public IHttpResponse response() {
    return this.httpServerResponse;
  }

  @Override
  public boolean cancelNext() {
    return this.cancelNext = true;
  }

  @Override
  public IHttpHandler peekLast() {
    return this.lastHandler;
  }

  @Override
  public IHttpComponent<IHttpServer> component() {
    return this.nettyHttpServer;
  }

  @Override
  public IHttpContext closeAfter(boolean value) {
    this.closeAfter = value;
    return this;
  }

  @Override
  public boolean closeAfter() {
    return this.closeAfter;
  }

  @Override
  public HttpCookie cookie(String name) {
    Preconditions.checkNotNull(name);

    return this.cookies.stream().filter(httpCookie -> httpCookie.getName().equalsIgnoreCase(name)).findFirst()
      .orElse(null);
  }

  @Override
  public Collection<HttpCookie> cookies() {
    return this.cookies;
  }

  @Override
  public boolean hasCookie(String name) {
    Preconditions.checkNotNull(name);

    return this.cookies.stream().anyMatch(httpCookie -> httpCookie.getName().equalsIgnoreCase(name));
  }

  @Override
  public IHttpContext setCookies(Collection<HttpCookie> cookies) {
    Preconditions.checkNotNull(cookies);

    this.cookies.clear();
    this.cookies.addAll(cookies);
    this.updateHeaderResponse();

    return this;
  }

  @Override
  public IHttpContext addCookie(HttpCookie httpCookie) {
    Preconditions.checkNotNull(httpCookie);

    HttpCookie cookie = this.cookie(httpCookie.getName());

    if (cookie != null) {
      this.removeCookie(cookie.getName());
    }
    this.cookies.add(httpCookie);
    this.updateHeaderResponse();

    return this;
  }

  @Override
  public IHttpContext removeCookie(String name) {
    Preconditions.checkNotNull(name);

    HttpCookie cookie = this.cookie(name);
    if (cookie != null) {
      cookie.setMaxAge(-1);
    }

    this.updateHeaderResponse();
    return this;
  }

  @Override
  public IHttpContext clearCookies() {
    this.cookies.clear();
    this.updateHeaderResponse();
    return this;
  }

  private void updateHeaderResponse() {
    if (this.cookies.isEmpty()) {
      this.httpServerResponse.httpResponse.headers().remove("Set-Cookie");
    } else {
      this.httpServerResponse.httpResponse.headers()
        .set("Set-Cookie", ServerCookieEncoder.LAX.encode(this.cookies.stream().map(httpCookie -> {
          Cookie cookie = new DefaultCookie(httpCookie.getName(), httpCookie.getValue());
          cookie.setDomain(httpCookie.getDomain());
          cookie.setMaxAge(httpCookie.getMaxAge());
          cookie.setPath(httpCookie.getPath());

          return cookie;
        }).collect(Collectors.toList())));
    }
  }

  public void setLastHandler(IHttpHandler lastHandler) {
    this.lastHandler = lastHandler;
  }
}
