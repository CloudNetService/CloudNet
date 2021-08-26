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

package de.dytanic.cloudnet.driver.network.http;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler.Whole;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.glassfish.tyrus.core.Utils;
import org.junit.jupiter.api.Assertions;

public class WebSocketClientEndpoint extends Endpoint {

  private final AtomicInteger eventCounter;

  public WebSocketClientEndpoint(AtomicInteger eventCounter) {
    this.eventCounter = eventCounter;
  }

  @Override
  public void onOpen(Session session, EndpointConfig config) {
    Assertions.assertEquals(0, this.eventCounter.getAndIncrement());

    session.addMessageHandler(new Whole<String>() { // this cannot be replaced with lambda - causes type lookup to fail
      @Override
      public void onMessage(String message) {
        try {
          Assertions.assertEquals(1, WebSocketClientEndpoint.this.eventCounter.getAndIncrement());
          Assertions.assertEquals("response", message);

          session.getAsyncRemote().sendPing(null);
        } catch (IOException exception) {
          Assertions.fail(exception);
        }
      }
    });
    session.addMessageHandler(new Whole<PongMessage>() { // see above
      @Override
      public void onMessage(PongMessage message) {
        Assertions.assertEquals(2, WebSocketClientEndpoint.this.eventCounter.getAndIncrement());

        String content = new String(Utils.getRemainingArray(message.getApplicationData()), StandardCharsets.UTF_8);
        Assertions.assertEquals("response2", content);

        session.getAsyncRemote().sendBinary(ByteBuffer.wrap(new byte[]{0, 5, 6}));
      }
    });

    session.getAsyncRemote().sendText("request");
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    Assertions.assertEquals(3, this.eventCounter.get());
    Assertions.assertEquals(CloseCodes.GOING_AWAY, closeReason.getCloseCode());
    Assertions.assertEquals("Successful close", closeReason.getReasonPhrase());
  }
}
