package de.dytanic.cloudnet.driver.network.netty.http;

import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class NettyWebSocketServerChannelHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final NettyWebSocketServerChannel webSocketServerChannel;

    public NettyWebSocketServerChannelHandler(NettyWebSocketServerChannel webSocketServerChannel) {
        this.webSocketServerChannel = webSocketServerChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
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

        if (webSocketFrame instanceof CloseWebSocketFrame) {
            this.webSocketServerChannel.close(200, "client connection closed");
        }
    }

    private void invoke0(WebSocketFrameType type, WebSocketFrame webSocketFrame) {
        byte[] bytes = this.readContentFromWebSocketFrame(webSocketFrame);

        for (IWebSocketListener listener : this.webSocketServerChannel.getListeners()) {
            try {
                listener.handle(this.webSocketServerChannel, type, bytes);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private byte[] readContentFromWebSocketFrame(WebSocketFrame frame) {
        int length = frame.content().readableBytes();

        if (frame.content().hasArray()) {
            return frame.content().array();
        } else {
            byte[] bytes = new byte[length];
            frame.content().getBytes(frame.content().readerIndex(), bytes);
            return bytes;
        }
    }

    public NettyWebSocketServerChannel getWebSocketServerChannel() {
        return this.webSocketServerChannel;
    }
}