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
import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyWebSocketServerChannel implements IWebSocketChannel {

  private final List<IWebSocketListener> webSocketListeners = new CopyOnWriteArrayList<>();

  private final IHttpChannel httpChannel;

  private final Channel channel;

  private final WebSocketServerHandshaker webSocketServerHandshaker;

  public NettyWebSocketServerChannel(IHttpChannel httpChannel, Channel channel,
    WebSocketServerHandshaker webSocketServerHandshaker) {
    this.httpChannel = httpChannel;
    this.channel = channel;
    this.webSocketServerHandshaker = webSocketServerHandshaker;
  }

  @Override
  public IWebSocketChannel addListener(IWebSocketListener... listeners) {
    Preconditions.checkNotNull(listeners);

    for (IWebSocketListener listener : listeners) {
      if (listener != null) {
        this.webSocketListeners.add(listener);
      }
    }

    return this;
  }

  @Override
  public IWebSocketChannel removeListener(IWebSocketListener... listeners) {
    Preconditions.checkNotNull(listeners);

    this.webSocketListeners.removeIf(listener -> Arrays.stream(listeners)
      .anyMatch(webSocketListener -> webSocketListener != null && webSocketListener.equals(listener)));

    return this;
  }

  @Override
  public IWebSocketChannel removeListener(Collection<Class<? extends IWebSocketListener>> classes) {
    Preconditions.checkNotNull(classes);

    this.webSocketListeners.removeIf(listener -> classes.contains(listener.getClass()));

    return this;
  }

  @Override
  public IWebSocketChannel removeListener(ClassLoader classLoader) {
    this.webSocketListeners.removeIf(listener -> listener.getClass().getClassLoader().equals(classLoader));

    return this;
  }

  @Override
  public IWebSocketChannel clearListeners() {
    this.webSocketListeners.clear();
    return this;
  }

  @Override
  public Collection<IWebSocketListener> getListeners() {
    return this.webSocketListeners;
  }

  @Override
  public IWebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, String text) {
    Preconditions.checkNotNull(webSocketFrameType);
    Preconditions.checkNotNull(text);

    return this.sendWebSocketFrame(webSocketFrameType, text.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public IWebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, byte[] bytes) {
    Preconditions.checkNotNull(webSocketFrameType);
    Preconditions.checkNotNull(bytes);

    WebSocketFrame webSocketFrame;

    switch (webSocketFrameType) {
      case PING:
        webSocketFrame = new PingWebSocketFrame(Unpooled.buffer(bytes.length).writeBytes(bytes));
        break;
      case PONG:
        webSocketFrame = new PongWebSocketFrame(Unpooled.buffer(bytes.length).writeBytes(bytes));
        break;
      case TEXT:
        webSocketFrame = new TextWebSocketFrame(Unpooled.buffer(bytes.length).writeBytes(bytes));
        break;
      default:
        webSocketFrame = new BinaryWebSocketFrame(Unpooled.buffer(bytes.length).writeBytes(bytes));
        break;
    }

    this.channel.writeAndFlush(webSocketFrame).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    return this;
  }

  @Override
  public IHttpChannel channel() {
    return this.httpChannel;
  }

  @Override
  public void close(int statusCode, String reasonText) {
    AtomicInteger statusCodeReference = new AtomicInteger(statusCode);
    AtomicReference<String> reasonTextReference = new AtomicReference<>(reasonText);

    for (IWebSocketListener listener : this.webSocketListeners) {
      listener.handleClose(this, statusCodeReference, reasonTextReference);
    }

    this.channel.writeAndFlush(new CloseWebSocketFrame(statusCodeReference.get(), reasonTextReference.get()))
      .addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void close() {
    this.close(200, "default closing");
  }

  public List<IWebSocketListener> getWebSocketListeners() {
    return this.webSocketListeners;
  }

  public IHttpChannel getHttpChannel() {
    return this.httpChannel;
  }

  public Channel getChannel() {
    return this.channel;
  }

  public WebSocketServerHandshaker getWebSocketServerHandshaker() {
    return this.webSocketServerHandshaker;
  }
}
