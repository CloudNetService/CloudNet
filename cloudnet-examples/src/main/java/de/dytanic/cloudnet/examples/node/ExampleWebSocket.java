/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.gson.GsonUtil;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketChannel;
import de.dytanic.cloudnet.driver.network.http.websocket.IWebSocketListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ExampleWebSocket {

  private final Collection<IWebSocketChannel> channels = new CopyOnWriteArrayList<>();

  @EventListener
  public void handlePostEventsToWebSocketChannels(Event event) {
    for (IWebSocketChannel channel : this.channels) {
      channel.sendWebSocketFrame(WebSocketFrameType.TEXT, GsonUtil.GSON.toJson(event));
    }
  }

  public void invokeWebSocketChannel() {
    CloudNet.getInstance().getHttpServer().registerHandler("/http_websocket_example_path", (path, context) -> {
      IWebSocketChannel channel = context.upgrade(); //upgraded context to WebSocket

      this.channels.add(channel);

      channel
        .addListener(new IWebSocketListener() { //Add a listener for received WebSocket channel messages and closing
          @Override
          public void handle(IWebSocketChannel channel, WebSocketFrameType type, byte[] bytes) {
            switch (type) {
              case PONG:
                channel
                  .sendWebSocketFrame(WebSocketFrameType.TEXT, new JsonDocument("message", "Hello, world!").toString());
                break;
              case TEXT:
                if ("handleClose".equals(new String(bytes))) {
                  channel.close(200, "invoked close");
                }
                break;
              default:
                break;
            }
          }

          @Override
          public void handleClose(IWebSocketChannel channel, AtomicInteger statusCode,
            AtomicReference<String> reasonText) { // handle the closing output
            if (!ExampleWebSocket.this.channels.contains(channel)) {
              statusCode.set(500);
            }

            ExampleWebSocket.this.channels.remove(channel);
            System.out.println("I close");
          }
        });

      channel.sendWebSocketFrame(WebSocketFrameType.PING, "Websocket Ping");
    });
  }
}
