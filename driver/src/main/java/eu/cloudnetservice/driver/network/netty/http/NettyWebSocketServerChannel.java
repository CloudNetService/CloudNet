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

import eu.cloudnetservice.driver.network.http.HttpChannel;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketChannel;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketFrameType;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketListener;
import io.netty5.buffer.DefaultBufferAllocators;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelFutureListeners;
import io.netty5.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.TextWebSocketFrame;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default netty based implementation of a web socket channel.
 *
 * @since 4.0
 */
final class NettyWebSocketServerChannel implements WebSocketChannel {

  private final Collection<WebSocketListener> webSocketListeners = ConcurrentHashMap.newKeySet();

  private final Channel channel;
  private final HttpChannel httpChannel;

  /**
   * Constructs a new netty web socket channel instance.
   *
   * @param httpChannel the original channel the upgrade request came from.
   * @param channel     the unwrapped netty channel.
   * @throws NullPointerException if either the given http or netty channel is null.
   */
  public NettyWebSocketServerChannel(@NonNull HttpChannel httpChannel, @NonNull Channel channel) {
    this.httpChannel = httpChannel;
    this.channel = channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull WebSocketChannel addListener(@NonNull WebSocketListener... listeners) {
    this.webSocketListeners.addAll(Arrays.asList(listeners));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull WebSocketChannel removeListener(@NonNull WebSocketListener... listeners) {
    this.webSocketListeners.removeIf(listener -> Arrays.asList(listeners).contains(listener));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull WebSocketChannel removeListener(@NonNull ClassLoader classLoader) {
    this.webSocketListeners.removeIf(listener -> listener.getClass().getClassLoader().equals(classLoader));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull WebSocketChannel clearListeners() {
    this.webSocketListeners.clear();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<WebSocketListener> listeners() {
    return this.webSocketListeners;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull WebSocketChannel sendWebSocketFrame(@NonNull WebSocketFrameType type, @NonNull String text) {
    return this.sendWebSocketFrame(type, text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull WebSocketChannel sendWebSocketFrame(@NonNull WebSocketFrameType webSocketFrameType, byte[] bytes) {
    var binaryData = DefaultBufferAllocators.offHeapAllocator().copyOf(bytes);
    var webSocketFrame = switch (webSocketFrameType) {
      case PING -> new PingWebSocketFrame(binaryData);
      case PONG -> new PongWebSocketFrame(binaryData);
      case TEXT -> new TextWebSocketFrame(binaryData);
      default -> new BinaryWebSocketFrame(binaryData);
    };

    this.channel
      .writeAndFlush(webSocketFrame)
      .addListener(this.channel, ChannelFutureListeners.FIRE_EXCEPTION_ON_FAILURE);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull HttpChannel channel() {
    return this.httpChannel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close(int statusCode, @Nullable String reasonText) {
    var statusCodeReference = new AtomicInteger(statusCode);
    var reasonTextReference = new AtomicReference<>(reasonText);

    for (var listener : this.webSocketListeners) {
      listener.handleClose(this, statusCodeReference, reasonTextReference);
    }

    this.channel
      .writeAndFlush(new CloseWebSocketFrame(
        DefaultBufferAllocators.offHeapAllocator(),
        statusCodeReference.get(),
        reasonTextReference.get()))
      .addListener(this.channel, ChannelFutureListeners.CLOSE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.close(1000, "goodbye");
  }
}
