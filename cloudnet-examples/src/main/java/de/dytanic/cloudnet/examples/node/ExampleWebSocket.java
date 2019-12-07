package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.gson.GsonUtil;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;

import java.util.Collection;

public class ExampleWebSocket {

    private final Collection<WebSocketChannel> channels = Iterables.newCopyOnWriteArrayList();

    @EventListener
    public void handlePostEventsToWebSocketChannels(Event event) {
        for (WebSocketChannel channel : channels) {
            channel.sendWebSocketFrame(WebSocketFrameType.TEXT, GsonUtil.GSON.toJson(event));
        }
    }

    public void invokeWebSocketChannel() {
        CloudNet.getInstance().getHttpServer().registerHandler("/http_websocket_example_path", (path, context) -> {
            WebSocketChannel channel = context.upgrade(); //upgraded context to WebSocket

            channels.add(channel);

            channel.addListener(new WebSocketListener() { //Add a listener for received WebSocket channel messages and closing
                @Override
                public void handle(WebSocketChannel channel, WebSocketFrameType type, byte[] bytes) {
                    switch (type) {
                        case PONG:
                            channel.sendWebSocketFrame(WebSocketFrameType.TEXT, new JsonDocument("message", "Hello, world!").toString());
                            break;
                        case TEXT:
                            if ("handleClose".equals(new String(bytes))) {
                                channel.close(200, "invoked close");
                            }
                            break;
                    }
                }

                @Override
                public void handleClose(WebSocketChannel channel, Value<Integer> statusCode, Value<String> reasonText) //handle the closing output
                {
                    if (!channels.contains(channel)) {
                        statusCode.setValue(500);
                    }

                    channels.remove(channel);

                    System.out.println("I close");
                }
            });

            channel.sendWebSocketFrame(WebSocketFrameType.PING, "Websocket Ping");
        });
    }
}