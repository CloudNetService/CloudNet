/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.http;

import eu.cloudnetservice.driver.network.NetworkTestCase;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketFrameType;
import eu.cloudnetservice.driver.network.netty.http.NettyHttpServer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@SuppressWarnings("NullableProblems")
public class NettyHttpServerWebSocketTest extends NetworkTestCase {

  private int serverPort;
  private HttpServer httpServer;

  @BeforeEach
  void initHttpServer() {
    this.serverPort = randomFreePort();

    this.httpServer = new NettyHttpServer();
    this.httpServer.addListener(this.serverPort).join();
  }

  @AfterEach
  void teardownHttpServer() throws Exception {
    this.httpServer.close();
  }

  @Test
  @Timeout(20)
  void testRespondsWithBadRequestWhenNotUpgrading() throws Exception {
    var client = HttpClient.newHttpClient();

    this.httpServer.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.upgrade().join();
        }
      });
    Assertions.assertEquals(1, this.httpServer.httpHandlers().size());

    var handlerUri = UriBuilder.create().port(this.serverPort).path("test").build();
    var handlerRequest = HttpRequest.newBuilder(handlerUri).build();
    var handlerResponse = client.send(handlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(426, handlerResponse.statusCode());
  }

  @Test
  @Timeout(20)
  void testWebsocketHandlerCanAccessRequestValues() {
    this.httpServer.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          var header = context.request().header("hello");
          Assertions.assertEquals("Test", header);
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter(true);
        }
      });

    var handlerUri = UriBuilder.create().scheme("ws").port(this.serverPort).path("test").build();
    var completionException = Assertions.assertThrows(CompletionException.class, () -> HttpClient.newHttpClient()
      .newWebSocketBuilder()
      .header("hello", "Test")
      .buildAsync(handlerUri, new WebSocket.Listener() {
        @Override
        public void onOpen(WebSocket webSocket) {
          Assertions.fail("Websocket connection should not open");
        }
      })
      .join());

    var realExceptionCause = completionException.getCause().getCause();
    Assertions.assertNotNull(realExceptionCause);
    Assertions.assertEquals("Unexpected HTTP response status code 200", realExceptionCause.getMessage());
  }

  @Test
  @Timeout(60)
  void testWebSocketHandlingWorksAsExpected() {
    var currentThread = Thread.currentThread();
    AtomicReference<Throwable> underlyingError = new AtomicReference<>();

    this.httpServer.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.upgrade().thenAccept(channel -> {
            // add a listener for web socket actions
            channel.addListener((requestChannel, frameType, data) -> {
              var decodedData = new String(data, StandardCharsets.UTF_8);
              switch (frameType) {
                case PING -> {
                  Assertions.assertEquals("You here?", decodedData);
                  requestChannel.sendWebSocketFrame(WebSocketFrameType.PONG, "Still here!");
                }
                case TEXT -> {
                  if (decodedData.equals("GO!")) {
                    requestChannel.close(1000, "Okay :/");
                    return;
                  }

                  Assertions.assertEquals("Hello World!", decodedData);
                  requestChannel.sendWebSocketFrame(WebSocketFrameType.BINARY, new byte[]{0x01, 0x02, 0x05, 0x09});
                }
                default -> Assertions.fail("Invalid request type received");
              }
            });
          });
        }
      });

    // open -> ping -> pong -> text (normal) -> binary -> text (request to close) -> close
    var handlerUri = UriBuilder.create().scheme("ws").port(this.serverPort).path("test").build();
    HttpClient.newHttpClient()
      .newWebSocketBuilder()
      .buildAsync(handlerUri, new WebSocket.Listener() {
        @Override
        public void onOpen(WebSocket webSocket) {
          var message = StandardCharsets.UTF_8.encode("You here?");
          webSocket.request(1);
          webSocket.sendPing(message).join();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
          Assertions.assertEquals(1000, statusCode);
          Assertions.assertEquals("Okay :/", reason);

          LockSupport.unpark(currentThread);
          return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
          underlyingError.set(error);
          LockSupport.unpark(currentThread);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
          Assertions.assertTrue(last);
          Assertions.assertEquals(0x01, data.get());
          Assertions.assertEquals(0x02, data.get());
          Assertions.assertEquals(0x05, data.get());
          Assertions.assertEquals(0x09, data.get());
          Assertions.assertFalse(data.hasRemaining());

          webSocket.request(1);
          webSocket.sendText("GO!", true).join();
          return null;
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
          var decodedData = StandardCharsets.UTF_8.decode(message).toString();
          Assertions.assertEquals("Still here!", decodedData);

          webSocket.request(1);
          webSocket.sendText("Hello World!", true).join();
          return null;
        }
      })
      .join();

    // wait for the test to finish or fail
    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(60));

    // fail the test with the thrown error (if any)
    var thrownError = underlyingError.get();
    if (thrownError != null) {
      Assertions.fail(thrownError);
    }
  }
}
