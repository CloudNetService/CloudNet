package de.dytanic.cloudnet.driver.network.http.websocket;

import de.dytanic.cloudnet.common.Value;

public interface WebSocketListener {

    void handle(WebSocketChannel channel, WebSocketFrameType type, byte[] bytes) throws Exception;

    default void handleClose(WebSocketChannel channel, Value<Integer> statusCode, Value<String> reasonText) {

    }
}