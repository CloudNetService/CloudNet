package de.dytanic.cloudnet.driver.network.netty;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

final class NettyWebSocketServerChannel implements IWebSocketChannel {

    private final List<IWebSocketListener> webSocketListeners = new CopyOnWriteArrayList<>();

    private final IHttpChannel httpChannel;

    private final Channel channel;

    private final WebSocketServerHandshaker webSocketServerHandshaker;

    public NettyWebSocketServerChannel(IHttpChannel httpChannel, Channel channel, WebSocketServerHandshaker webSocketServerHandshaker) {
        this.httpChannel = httpChannel;
        this.channel = channel;
        this.webSocketServerHandshaker = webSocketServerHandshaker;
    }

    @Override
    public IWebSocketChannel addListener(IWebSocketListener... listeners) {
        Preconditions.checkNotNull(listeners);

        for (IWebSocketListener listener : listeners) {
            if (listener != null) {
                webSocketListeners.add(listener);
            }
        }

        return this;
    }

    @Override
    public IWebSocketChannel removeListener(IWebSocketListener... listeners) {
        Preconditions.checkNotNull(listeners);

        webSocketListeners.removeIf(listener -> Arrays.stream(listeners).anyMatch(webSocketListener -> webSocketListener != null && webSocketListener.equals(listener)));

        return this;
    }

    @Override
    public IWebSocketChannel removeListener(Collection<Class<? extends IWebSocketListener>> classes) {
        Preconditions.checkNotNull(classes);

        webSocketListeners.removeIf(listener -> classes.contains(listener.getClass()));

        return this;
    }

    @Override
    public IWebSocketChannel removeListener(ClassLoader classLoader) {
        webSocketListeners.removeIf(listener -> listener.getClass().getClassLoader().equals(classLoader));

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

        channel.writeAndFlush(webSocketFrame).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        return this;
    }

    @Override
    public IHttpChannel channel() {
        return httpChannel;
    }

    @Override
    public void close(int statusCode, String reasonText) {
        Preconditions.checkNotNull(statusCode);

        AtomicReference<Integer> statusCodeReference = new AtomicReference<>(statusCode);
        AtomicReference<String> reasonTextReference = new AtomicReference<>(reasonText);

        for (IWebSocketListener listener : webSocketListeners) {
            listener.handleClose(this, statusCodeReference, reasonTextReference);
        }

        this.channel.writeAndFlush(new CloseWebSocketFrame(statusCodeReference.get(), reasonTextReference.get())).addListener(ChannelFutureListener.CLOSE);
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