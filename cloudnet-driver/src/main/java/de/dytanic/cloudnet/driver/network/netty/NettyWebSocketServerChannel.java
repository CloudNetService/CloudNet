package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.network.http.HttpChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.*;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

final class NettyWebSocketServerChannel implements WebSocketChannel {

    private final List<WebSocketListener> webSocketListeners = Iterables.newCopyOnWriteArrayList();

    private final HttpChannel httpChannel;

    private final Channel channel;

    private final WebSocketServerHandshaker webSocketServerHandshaker;

    public NettyWebSocketServerChannel(HttpChannel httpChannel, Channel channel, WebSocketServerHandshaker webSocketServerHandshaker) {
        this.httpChannel = httpChannel;
        this.channel = channel;
        this.webSocketServerHandshaker = webSocketServerHandshaker;
    }

    @Override
    public WebSocketChannel addListener(WebSocketListener... listeners) {
        Validate.checkNotNull(listeners);

        for (WebSocketListener listener : listeners) {
            if (listener != null) {
                webSocketListeners.add(listener);
            }
        }

        return this;
    }

    @Override
    public WebSocketChannel removeListener(WebSocketListener... listeners) {
        Validate.checkNotNull(listeners);

        for (WebSocketListener listener : webSocketListeners) {
            if (Iterables.first(listeners, webSocketListener -> webSocketListener != null && webSocketListener.equals(listener)) != null) {
                webSocketListeners.remove(listener);
            }
        }

        return this;
    }

    @Override
    public WebSocketChannel removeListener(Collection<Class<? extends WebSocketListener>> classes) {
        Validate.checkNotNull(classes);

        for (WebSocketListener listener : webSocketListeners) {
            if (classes.contains(listener.getClass())) {
                webSocketListeners.remove(listener);
            }
        }

        return this;
    }

    @Override
    public WebSocketChannel removeListener(ClassLoader classLoader) {
        for (WebSocketListener listener : webSocketListeners) {
            if (listener.getClass().getClassLoader().equals(classLoader)) {
                webSocketListeners.remove(listener);
            }
        }

        return this;
    }

    @Override
    public WebSocketChannel clearListeners() {
        this.webSocketListeners.clear();
        return this;
    }

    @Override
    public Collection<WebSocketListener> getListeners() {
        return this.webSocketListeners;
    }

    @Override
    public WebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, String text) {
        Validate.checkNotNull(webSocketFrameType);
        Validate.checkNotNull(text);

        return this.sendWebSocketFrame(webSocketFrameType, text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public WebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, byte[] bytes) {
        Validate.checkNotNull(webSocketFrameType);
        Validate.checkNotNull(bytes);

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

        channel.writeAndFlush(webSocketFrame).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        return this;
    }

    @Override
    public HttpChannel channel() {
        return httpChannel;
    }

    @Override
    public void close(int statusCode, String reasonText) {
        Validate.checkNotNull(statusCode);

        Value<Integer> statusCodeWrapper = new Value<>(statusCode);
        Value<String> reasonTextWrapper = new Value<>(reasonText);

        for (WebSocketListener listener : webSocketListeners) {
            listener.handleClose(this, statusCodeWrapper, reasonTextWrapper);
        }

        this.channel.writeAndFlush(new CloseWebSocketFrame(statusCodeWrapper.getValue(), reasonTextWrapper.getValue())).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void close() {
        this.close(200, "default closing");
    }

    public List<WebSocketListener> getWebSocketListeners() {
        return this.webSocketListeners;
    }

    public HttpChannel getHttpChannel() {
        return this.httpChannel;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public WebSocketServerHandshaker getWebSocketServerHandshaker() {
        return this.webSocketServerHandshaker;
    }
}