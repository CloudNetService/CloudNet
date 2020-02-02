package de.dytanic.cloudnet.driver.network.http.websocket;

import java.util.concurrent.atomic.AtomicReference;

public interface IWebSocketListener {

    void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) throws Exception;

    default void handleClose(IWebSocketChannel channel, AtomicReference<Integer> statusCode, AtomicReference<String> reasonText) {

    }
}