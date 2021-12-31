/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.http;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.glassfish.tyrus.core.Utils;
import org.junit.jupiter.api.Assertions;

@ClientEndpoint
public class WebSocketClientEndpoint {

  private final AtomicInteger eventCounter = new AtomicInteger();

  @OnOpen
  public void onOpen(Session session) {
    Assertions.assertEquals(0, this.eventCounter.getAndIncrement());
    session.getAsyncRemote().sendText("request");
  }

  @OnMessage
  public void onStringMessage(Session session, String message) throws IOException {
    Assertions.assertEquals(1, WebSocketClientEndpoint.this.eventCounter.getAndIncrement());
    Assertions.assertEquals("response", message);

    session.getAsyncRemote().sendPing(null);
  }

  @OnMessage
  public void onPongMessage(Session session, PongMessage message) {
    Assertions.assertEquals(2, WebSocketClientEndpoint.this.eventCounter.getAndIncrement());

    var content = new String(Utils.getRemainingArray(message.getApplicationData()), StandardCharsets.UTF_8);
    Assertions.assertEquals("response2", content);

    session.getAsyncRemote().sendBinary(ByteBuffer.wrap(new byte[]{0, 5, 6}));
  }

  @OnClose
  public void onClose(CloseReason closeReason) {
    Assertions.assertEquals(3, this.eventCounter.get());
    Assertions.assertEquals(CloseCodes.GOING_AWAY, closeReason.getCloseCode());
    Assertions.assertEquals("Successful close", closeReason.getReasonPhrase());
  }
}
