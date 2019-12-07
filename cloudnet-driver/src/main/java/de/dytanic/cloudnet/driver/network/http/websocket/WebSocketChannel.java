package de.dytanic.cloudnet.driver.network.http.websocket;

import de.dytanic.cloudnet.driver.network.http.HttpChannel;

import java.util.Collection;

public interface WebSocketChannel extends AutoCloseable {

    WebSocketChannel addListener(WebSocketListener... listeners);

    WebSocketChannel removeListener(WebSocketListener... listeners);

    WebSocketChannel removeListener(Collection<Class<? extends WebSocketListener>> classes);

    WebSocketChannel removeListener(ClassLoader classLoader);

    WebSocketChannel clearListeners();

    Collection<WebSocketListener> getListeners();

    WebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, String text);

    WebSocketChannel sendWebSocketFrame(WebSocketFrameType webSocketFrameType, byte[] bytes);

    void close(int statusCode, String reasonText);

    HttpChannel channel();

}