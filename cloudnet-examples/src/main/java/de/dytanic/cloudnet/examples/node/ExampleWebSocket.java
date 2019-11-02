package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.gson.GsonUtil;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;

import java.util.Collection;

public class ExampleWebSocket {

    private final Collection<IWebSocketChannel> channels = Iterables.newCopyOnWriteArrayList();

    @EventListener
    public void handlePostEventsToWebSocketChannels(Event event) {
        for (IWebSocketChannel channel : channels) {
            channel.sendWebSocketFrame(WebSocketFrameType.TEXT, GsonUtil.GSON.toJson(event));
        }
    }

    public void invokeWebSocketChannel() {
        CloudNet.getInstance().getHttpServer().registerHandler("/http_websocket_example_path", (path, context) -> {
            IWebSocketChannel channel = context.upgrade(); //upgraded context to WebSocket

            channels.add(channel);

            channel.addListener(new IWebSocketListener() { //Add a listener for received WebSocket channel messages and closing
                @Override
                public void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) {
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
                public void handleClose(IWebSocketChannel channel, Value<Integer> statusCode, Value<String> reasonText) //handle the closing output
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