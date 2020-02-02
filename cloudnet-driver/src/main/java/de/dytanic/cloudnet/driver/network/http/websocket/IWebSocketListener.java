package de.dytanic.cloudnet.driver.network.http.websocket;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public interface IWebSocketListener {

    void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) throws Exception;

    default void handleClose(IWebSocketChannel channel, AtomicInteger statusCode, AtomicReference<String> reasonText) {

    }
}