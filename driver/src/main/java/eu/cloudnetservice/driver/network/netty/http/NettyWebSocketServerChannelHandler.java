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

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketFrameType;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import io.netty5.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.WebSocketFrame;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * The default netty based handler for web socket messages.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class NettyWebSocketServerChannelHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

  private static final Logger LOGGER = LogManager.logger(NettyWebSocketServerChannelHandler.class);

  private final NettyWebSocketServerChannel webSocketServerChannel;

  /**
   * Constructs a new web socket server handler instance.
   *
   * @param webSocketServerChannel the wrapped web socket channel.
   * @throws NullPointerException if the channel to wrap is null.
   */
  public NettyWebSocketServerChannelHandler(@NonNull NettyWebSocketServerChannel webSocketServerChannel) {
    this.webSocketServerChannel = webSocketServerChannel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelExceptionCaught(@NonNull ChannelHandlerContext ctx, @NonNull Throwable cause) {
    if (!(cause instanceof IOException)) {
      LOGGER.severe("Exception was caught", cause);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelReadComplete(@NonNull ChannelHandlerContext ctx) {
    ctx.flush();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelInactive(@NonNull ChannelHandlerContext ctx) {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      ctx.channel().close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void messageReceived(@NonNull ChannelHandlerContext ctx, @NonNull WebSocketFrame webSocketFrame) {
    if (webSocketFrame instanceof PingWebSocketFrame) {
      this.invoke0(WebSocketFrameType.PING, webSocketFrame);
    }

    if (webSocketFrame instanceof PongWebSocketFrame) {
      this.invoke0(WebSocketFrameType.PONG, webSocketFrame);
    }

    if (webSocketFrame instanceof TextWebSocketFrame) {
      this.invoke0(WebSocketFrameType.TEXT, webSocketFrame);
    }

    if (webSocketFrame instanceof BinaryWebSocketFrame) {
      this.invoke0(WebSocketFrameType.BINARY, webSocketFrame);
    }

    if (webSocketFrame instanceof CloseWebSocketFrame frame) {
      this.webSocketServerChannel.close(frame.statusCode(), frame.reasonText());
    }
  }

  /**
   * Invokes all handlers when a web socket frame was received.
   *
   * @param type           the type of frame received.
   * @param webSocketFrame the actual frame which was received.
   * @throws NullPointerException if either the given type of frame is null.
   */
  private void invoke0(@NonNull WebSocketFrameType type, @NonNull WebSocketFrame webSocketFrame) {
    var bytes = this.readContentFromWebSocketFrame(webSocketFrame);
    for (var listener : this.webSocketServerChannel.listeners()) {
      try {
        listener.handle(this.webSocketServerChannel, type, bytes);
      } catch (Exception exception) {
        LOGGER.severe("Exception while invoking handle", exception);
      }
    }
  }

  /**
   * Converts the body of a web socket frame to a byte array.
   *
   * @param frame the received frame to read the body from.
   * @return the body of the frame, in bytes.
   * @throws NullPointerException if the given frame is null.
   */
  private byte[] readContentFromWebSocketFrame(@NonNull WebSocketFrame frame) {
    var bytes = new byte[frame.binaryData().readableBytes()];
    frame.binaryData().readBytes(bytes, 0, bytes.length);
    return bytes;
  }
}
