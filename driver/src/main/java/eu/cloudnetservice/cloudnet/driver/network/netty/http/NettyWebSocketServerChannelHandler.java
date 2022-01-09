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
import eu.cloudnetservice.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class NettyWebSocketServerChannelHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

  private static final Logger LOGGER = LogManager.logger(NettyWebSocketServerChannelHandler.class);

  private final NettyWebSocketServerChannel webSocketServerChannel;

  public NettyWebSocketServerChannelHandler(NettyWebSocketServerChannel webSocketServerChannel) {
    this.webSocketServerChannel = webSocketServerChannel;
  }

  @Override
  public void exceptionCaught(@NonNull ChannelHandlerContext ctx, @NonNull Throwable cause) {
    if (!(cause instanceof IOException)) {
      LOGGER.severe("Exception was caught", cause);
    }
  }

  @Override
  public void channelReadComplete(@NonNull ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelInactive(@NonNull ChannelHandlerContext ctx) {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      ctx.channel().close();
    }
  }

  @Override
  protected void channelRead0(@NonNull ChannelHandlerContext ctx, @NonNull WebSocketFrame webSocketFrame) {
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

  private byte[] readContentFromWebSocketFrame(@NonNull WebSocketFrame frame) {
    var bytes = new byte[frame.content().readableBytes()];
    frame.content().getBytes(frame.content().readerIndex(), bytes);
    return bytes;
  }
}
