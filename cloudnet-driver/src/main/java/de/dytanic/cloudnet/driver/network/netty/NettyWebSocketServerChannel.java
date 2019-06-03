package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.network.http.IHttpChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
final class NettyWebSocketServerChannel implements IWebSocketChannel {

    private final List<IWebSocketListener> webSocketListeners = Iterables.newCopyOnWriteArrayList();

    private final IHttpChannel httpChannel;

    private final Channel channel;

    private final WebSocketServerHandshaker webSocketServerHandshaker;

    @Override
    public IWebSocketChannel addListener(IWebSocketListener... listeners)
    {
        Validate.checkNotNull(listeners);

        for (IWebSocketListener listener : listeners)
            if (listener != null)
                webSocketListeners.add(listener);

        return this;
    }

    @Override
    public IWebSocketChannel removeListener(IWebSocketListener... listeners)
    {
        Validate.checkNotNull(listeners);

        for (IWebSocketListener listener : webSocketListeners)
            if (Iterables.first(listeners, new Predicate<IWebSocketListener>() {
                @Override
                public boolean test(IWebSocketListener webSocketListener)
                {
                    return webSocketListener != null && webSocketListener.equals(listener);
                }
            }) != null)
                webSocketListeners.remove(listener);

        return this;
    }

    @Override
    public IWebSocketChannel removeListener(Collection<Class<? extends IWebSocketListener>> classes)
    {
        Validate.checkNotNull(classes);

        for (IWebSocketListener listener : webSocketListeners)
            if (classes.contains(listener.getClass()))
                webSocketListeners.remove(listener);

        return this;
    }

    @Override
    public IWebSocketChannel removeListener(ClassLoader classLoader)
    {
        for (IWebSocketListener listener : webSocketListeners)
            if (listener.getClass().getClassLoader().equals(classLoader))
                webSocketListeners.remove(listener);

        return this;
    }

    @Override
    public IWebSocketChannel clearListeners()
    {
        this.webSocketListeners.clear();
        return this;
    }

    @Override
    public Collection<IWebSocketListener> getListeners()
    {
        return this.webSocketListeners;
    }

    @Override
    public IWebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, String text)
    {
        Validate.checkNotNull(webSocketFrameType);
        Validate.checkNotNull(text);

        return this.sendWebSocketFrame(webSocketFrameType, text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IWebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, byte[] bytes)
    {
        Validate.checkNotNull(webSocketFrameType);
        Validate.checkNotNull(bytes);

        WebSocketFrame webSocketFrame;

        switch (webSocketFrameType)
        {
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
    public IHttpChannel channel()
    {
        return httpChannel;
    }

    @Override
    public void close(int statusCode, String reasonText)
    {
        Validate.checkNotNull(statusCode);

        Value<Integer> statusCodeWrapper = new Value<>(statusCode);
        Value<String> reasonTextWrapper = new Value<>(reasonText);

        for (IWebSocketListener listener : webSocketListeners)
        {
            listener.handleClose(this, statusCodeWrapper, reasonTextWrapper);
        }

        this.channel.writeAndFlush(new CloseWebSocketFrame(statusCodeWrapper.getValue(), reasonTextWrapper.getValue())).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void close() throws Exception
    {
        this.close(200, "default closing");
    }
}