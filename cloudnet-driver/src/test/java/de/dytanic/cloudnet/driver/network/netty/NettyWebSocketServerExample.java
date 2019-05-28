package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
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
import java.util.concurrent.Callable;
import org.junit.Assert;
import org.junit.Test;

public final class NettyWebSocketServerExample {

  private static final String
      PING_STRING = "ping value",
      PONG_STRING = "pong value",
      TEXT_STRING = "text value",
      BINARY_STRING = "binary value";

  private static final ITask<String> task = new ListenableTask<>(
      new Callable<String>() {
        @Override
        public String call() throws Exception {
          return "Async_response_message";
        }
      });

  @Test
  public void testWebSocket() throws Exception {
    IHttpServer httpServer = new NettyHttpServer();

    httpServer.registerHandler("/test", new IHttpHandler() {
      @Override
      public void handle(String path, IHttpContext context) throws Exception {
        IWebSocketChannel channel = context.upgrade();
        channel.addListener(new IWebSocketListener() {
          @Override
          public void handle(IWebSocketChannel channel, WebSocketFrameType type,
              byte[] bytes) throws Exception {
            switch (type) {
              case PING:
                if (new String(bytes, StandardCharsets.UTF_8)
                    .equals(PING_STRING)) {
                  channel
                      .sendWebSocketFrame(WebSocketFrameType.PONG, PONG_STRING);
                }
                break;
              case TEXT:
                if (new String(bytes, StandardCharsets.UTF_8)
                    .equals(TEXT_STRING)) {
                  channel.sendWebSocketFrame(WebSocketFrameType.BINARY,
                      BINARY_STRING);
                }
                break;
              case CLOSE:
                channel.close();
                break;
            }
          }
        });

        context.cancelNext();
      }
    }).addListener(new HostAndPort("0.0.0.0", 4044));

    EventLoopGroup eventLoopGroup = NettyUtils.newEventLoopGroup();
    WebSocketClientHandshaker webSocketClientHandshaker = WebSocketClientHandshakerFactory
        .newHandshaker(
            new URI("ws://127.0.0.1:4044/test"),
            WebSocketVersion.V13,
            null,
            false,
            HttpHeaders.EMPTY_HEADERS,
            1280000
        );

    new Bootstrap()
        .group(eventLoopGroup)
        .channel(NettyUtils.getSocketChannelClass())
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(
                new HttpClientCodec(),
                new HttpObjectAggregator(65536),
                WebSocketClientCompressionHandler.INSTANCE,
                new ChannelInboundHandlerAdapter() {

                  @Override
                  public void channelActive(ChannelHandlerContext ctx)
                      throws Exception {
                    webSocketClientHandshaker.handshake(ctx.channel());
                  }

                  @Override
                  public void channelRead(ChannelHandlerContext ctx, Object msg)
                      throws Exception {
                    if (!webSocketClientHandshaker.isHandshakeComplete()
                        && msg instanceof FullHttpResponse) {
                      try {
                        webSocketClientHandshaker.finishHandshake(ctx.channel(),
                            (FullHttpResponse) msg);
                      } catch (Exception ex) {
                        ex.printStackTrace();
                      }

                      ctx.channel().eventLoop().execute(new Runnable() {
                        @Override
                        public void run() {
                          ctx.channel().writeAndFlush(new PingWebSocketFrame(
                              Unpooled.buffer()
                                  .writeBytes(PING_STRING.getBytes())));
                        }
                      });
                      return;
                    }

                    if (msg instanceof WebSocketFrame) {
                      WebSocketFrame webSocketFrame = (WebSocketFrame) msg;

                      if (msg instanceof PongWebSocketFrame) {
                        if (PONG_STRING.equals(webSocketFrame.content()
                            .toString(StandardCharsets.UTF_8))) {
                          ctx.channel().writeAndFlush(new TextWebSocketFrame(
                              Unpooled.buffer()
                                  .writeBytes(TEXT_STRING.getBytes())));
                        }
                        return;
                      }

                      if (msg instanceof BinaryWebSocketFrame) {
                        if (BINARY_STRING.equals(webSocketFrame.content()
                            .toString(StandardCharsets.UTF_8))) {
                          ctx.channel().writeAndFlush(new CloseWebSocketFrame())
                              .addListener(ChannelFutureListener.CLOSE);
                          task.call();
                        }
                      }
                    }
                  }
                });
          }
        })
        .connect("127.0.0.1", 4044)
        .sync()
        .channel()
    ;

    Assert.assertEquals("Async_response_message", task.get());
    eventLoopGroup.shutdownGracefully();
    httpServer.close();
  }
}