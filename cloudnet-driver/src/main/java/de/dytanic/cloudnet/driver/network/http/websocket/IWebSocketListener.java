package de.dytanic.cloudnet.driver.network.http.websocket;

import de.dytanic.cloudnet.common.Value;

public interface IWebSocketListener {

    void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) throws Exception;

    default void handleClose(IWebSocketChannel channel, Value<Integer> statusCode, Value<String> reasonText) {

    }
}