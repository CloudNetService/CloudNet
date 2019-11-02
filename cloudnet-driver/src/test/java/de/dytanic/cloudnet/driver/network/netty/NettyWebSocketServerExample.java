package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class NettyWebSocketServerExample {

    private static final String
            PING_STRING = "ping value",
            PONG_STRING = "pong value",
            TEXT_STRING = "text value",
            BINARY_STRING = "binary value";

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
                }
            });

            context.cancelNext();
        }).addListener(new HostAndPort("0.0.0.0", port));

        EventLoopGroup eventLoopGroup = NettyUtils.newEventLoopGroup();
        WebSocketClientHandshaker webSocketClientHandshaker = WebSocketClientHandshakerFactory
                .newHandshaker(
                        new URI("ws://127.0.0.1:" + port + "/test"),
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
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                        webSocketClientHandshaker.handshake(ctx.channel());
                                    }

                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        if (!webSocketClientHandshaker.isHandshakeComplete() && msg instanceof FullHttpResponse) {
                                            try {
                                                webSocketClientHandshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
                                            } catch (Exception exception) {
                                                exception.printStackTrace();
                                            }

                                            ctx.channel().eventLoop().execute(() -> ctx.channel().writeAndFlush(new PingWebSocketFrame(Unpooled.buffer().writeBytes(PING_STRING.getBytes()))));
                                            return;
                                        }

                                        if (msg instanceof WebSocketFrame) {
                                            WebSocketFrame webSocketFrame = (WebSocketFrame) msg;

                                            if (msg instanceof PongWebSocketFrame) {
                                                if (PONG_STRING.equals(webSocketFrame.content().toString(StandardCharsets.UTF_8))) {
                                                    ctx.channel().writeAndFlush(new TextWebSocketFrame(Unpooled.buffer().writeBytes(TEXT_STRING.getBytes())));
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

        Assert.assertEquals("Async_response_message", task.get());
        eventLoopGroup.shutdownGracefully();
        httpServer.close();
    }
}