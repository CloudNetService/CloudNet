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

package de.dytanic.cloudnet.driver.network.netty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import de.dytanic.cloudnet.driver.network.netty.http.NettyHttpServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public final class NettyWebSocketServerExample {

  private static final String PING_STRING = "ping value";
  private static final String PONG_STRING = "pong value";
  private static final String TEXT_STRING = "text value";
  private static final String BINARY_STRING = "binary value";

  private static final ITask<String> task = new ListenableTask<>(() -> "Async_response_message");

  @Test
  public void testWebSocket() throws Exception {
    int port = NettyTestUtil.generateRandomPort();

    IHttpServer httpServer = new NettyHttpServer();

    httpServer.registerHandler("/test", (path, context) -> {
      IWebSocketChannel channel = context.upgrade();
      channel.addListener((channel1, type, bytes) -> {
        switch (type) {
          case PING:
            if (new String(bytes, StandardCharsets.UTF_8).equals(PING_STRING)) {
              channel1.sendWebSocketFrame(WebSocketFrameType.PONG, PONG_STRING);
            }
            break;
          case TEXT:
            if (new String(bytes, StandardCharsets.UTF_8).equals(TEXT_STRING)) {
              channel1.sendWebSocketFrame(WebSocketFrameType.BINARY, BINARY_STRING);
            }
            break;
          case CLOSE:
            channel1.close();
            break;
          default:
            break;
        }
      });

      context.cancelNext();
    });

    assertTrue(httpServer.addListener(new HostAndPort("127.0.0.1", port)));

    EventLoopGroup eventLoopGroup = NettyUtils.newEventLoopGroup();
    WebSocketClientHandshaker webSocketClientHandshaker = WebSocketClientHandshakerFactory
      .newHandshaker(
        new URI("ws://127.0.0.1:" + port + "/test"),
        WebSocketVersion.V13,
        null,
        false,
        EmptyHttpHeaders.INSTANCE,
        1280000
      );

    new Bootstrap()
      .group(eventLoopGroup)
      .channelFactory(NettyUtils.getClientChannelFactory())
      .handler(new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(@NotNull Channel ch) {
          ch.pipeline().addLast(
            new HttpClientCodec(),
            new HttpObjectAggregator(65536),
            WebSocketClientCompressionHandler.INSTANCE,
            new ChannelInboundHandlerAdapter() {

              @Override
              public void channelActive(@NotNull ChannelHandlerContext ctx) {
                webSocketClientHandshaker.handshake(ctx.channel());
              }

              @Override
              public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
                if (!webSocketClientHandshaker.isHandshakeComplete() && msg instanceof FullHttpResponse) {
                  try {
                    webSocketClientHandshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
                  } catch (Exception exception) {
                    exception.printStackTrace();
                  }

                  ctx.channel().eventLoop().execute(() -> ctx.channel()
                    .writeAndFlush(new PingWebSocketFrame(Unpooled.buffer().writeBytes(PING_STRING.getBytes()))));
                  return;
                }

                if (msg instanceof WebSocketFrame) {
                  WebSocketFrame webSocketFrame = (WebSocketFrame) msg;

                  if (msg instanceof PongWebSocketFrame) {
                    if (PONG_STRING.equals(webSocketFrame.content().toString(StandardCharsets.UTF_8))) {
                      ctx.channel()
                        .writeAndFlush(new TextWebSocketFrame(Unpooled.buffer().writeBytes(TEXT_STRING.getBytes())));
                    }
                    return;
                  }

                  if (msg instanceof BinaryWebSocketFrame) {
                    if (BINARY_STRING.equals(webSocketFrame.content().toString(StandardCharsets.UTF_8))) {
                      ctx.channel().writeAndFlush(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
                      task.call();
                    }
                  }
                }
              }
            });
        }
      })
      .connect("127.0.0.1", port)
      .sync()
      .channel()
    ;

    assertEquals("Async_response_message", task.get());
    eventLoopGroup.shutdownGracefully();
    httpServer.close();
  }
}
