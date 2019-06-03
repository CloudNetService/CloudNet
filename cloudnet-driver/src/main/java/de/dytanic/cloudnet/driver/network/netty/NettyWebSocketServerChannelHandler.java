package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
final class NettyWebSocketServerChannelHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final NettyWebSocketServerChannel webSocketServerChannel;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) throws Exception {
        if (webSocketFrame instanceof PingWebSocketFrame)
            invoke0(WebSocketFrameType.PING, webSocketFrame);

        if (webSocketFrame instanceof PongWebSocketFrame)
            invoke0(WebSocketFrameType.PONG, webSocketFrame);

        if (webSocketFrame instanceof TextWebSocketFrame)
            invoke0(WebSocketFrameType.TEXT, webSocketFrame);

        if (webSocketFrame instanceof BinaryWebSocketFrame)
            invoke0(WebSocketFrameType.BINARY, webSocketFrame);

        if (webSocketFrame instanceof CloseWebSocketFrame)
            webSocketServerChannel.close(200, "client connection closed");
    }

    private void invoke0(WebSocketFrameType type, WebSocketFrame webSocketFrame) {
        byte[] bytes = readContentFromWebSocketFrame(webSocketFrame);

        for (IWebSocketListener listener : webSocketServerChannel.getListeners()) {
            try {
                listener.handle(webSocketServerChannel, type, bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] readContentFromWebSocketFrame(WebSocketFrame frame) {
        int length = frame.content().readableBytes();

        if (frame.content().hasArray())
            return frame.content().array();
        else {
            byte[] bytes = new byte[length];
            frame.content().getBytes(frame.content().readerIndex(), bytes);
            return bytes;
        }
    }
}