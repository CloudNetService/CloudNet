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

import com.google.common.collect.Iterables;
import eu.cloudnetservice.common.function.ThrowableConsumer;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.network.NetworkTestCase;
import eu.cloudnetservice.driver.network.http.websocket.WebSocketFrameType;
import eu.cloudnetservice.driver.network.netty.http.NettyHttpServer;
import io.netty5.handler.codec.http.headers.DefaultHttpCookiePair;
import io.netty5.handler.codec.http.headers.DefaultHttpSetCookie;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import org.glassfish.tyrus.client.ClientManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

@SuppressWarnings("NullableProblems")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NettyHttpServerTest extends NetworkTestCase {

  private static final List<String> SUPPORTED_METHODS = Arrays.asList(
    "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE");

  static HttpURLConnection connectTo(int port, String path) throws Exception {
    return connectTo(port, path, $ -> {
    });
  }

  static HttpURLConnection connectTo(
    int port,
    String path,
    ThrowableConsumer<HttpURLConnection, Exception> modifier
  ) throws Exception {
    var connection = (HttpURLConnection) httpUrl(port, path).openConnection();
    connection.setUseCaches(false);
    connection.setReadTimeout(5000);
    connection.setConnectTimeout(5000);

    modifier.accept(connection);
    connection.connect();
    return connection;
  }

  static URL httpUrl(int port, String path) throws Exception {
    return new URL(String.format("http://127.0.0.1:%d/%s", port, path == null ? "" : path));
  }

  @Test
  @Order(0)
  void testHttpHandlerRegister() {
    HttpServer server = new NettyHttpServer();

    server.registerHandler("/users/info", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {

      }
    });
    server.registerHandler("users/{user}/info", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {

      }
    });

    Assertions.assertEquals(2, server.httpHandlers().size());

    server.removeHandler(this.getClass().getClassLoader());
    Assertions.assertEquals(0, server.httpHandlers().size());
  }

  @Test
  @Order(10)
  void testDuplicateServerBind() {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    Assertions.assertFalse(server.sslEnabled());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());
    Assertions.assertThrows(CompletionException.class, () -> server.addListener(port).join());

    Assertions.assertDoesNotThrow(server::close);
  }

  @Test
  @Order(20)
  void testHttpRequestMethods() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    var handledTypes = new AtomicInteger();
    server.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          Assertions.assertEquals(SUPPORTED_METHODS.get(handledTypes.getAndIncrement()), context.request().method());
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter();
        }
      });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    for (var supportedMethod : SUPPORTED_METHODS) {
      var connection = connectTo(port, "test", con -> con.setRequestMethod(supportedMethod));
      connection.connect();
      Assertions.assertEquals(200, connection.getResponseCode());
    }
  }

  @Test
  @Order(25)
  @Timeout(20)
  void testRootHandler() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler("/", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {
        context.response().status(HttpResponseCode.OK).context().cancelNext(true);
      }
    });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    Assertions.assertEquals(200, connectTo(port, "").getResponseCode());
    Assertions.assertEquals(404, connectTo(port, "test").getResponseCode());
  }

  @Test
  @Order(30)
  void testRequestPathParameters() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test/{id}",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          Assertions.assertEquals("1234", context.request().pathParameters().get("id"));
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter();
        }
      });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    Assertions.assertEquals(200, connectTo(port, "test/1234").getResponseCode());
    Assertions.assertEquals(404, connectTo(port, "test1234").getResponseCode());
  }

  @Test
  @Order(40)
  void testRequestQueryParameters() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          Assertions.assertEquals("1234", Iterables.getFirst(context.request().queryParameters().get("id"), null));
          Assertions.assertIterableEquals(Arrays.asList("1", "2"), context.request().queryParameters().get("array"));
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter();
        }
      });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    Assertions.assertEquals(200, connectTo(port, "test?id=1234&array=1&array=2").getResponseCode());
    Assertions.assertEquals(404, connectTo(port, "test1234").getResponseCode());
  }

  @Test
  @Order(50)
  void testHandlerPriority() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test1",
      HttpHandler.PRIORITY_LOW,
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          var cancelNext = Boolean.parseBoolean(
            Iterables.getFirst(context.request().queryParameters().get("cancelNext"), null));
          context.response().status(HttpResponseCode.OK).context().cancelNext(cancelNext);
        }
      });
    server.registerHandler(
      "/test1",
      HttpHandler.PRIORITY_HIGH,
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.response().status(HttpResponseCode.CREATED).context().closeAfter();
        }
      });

    Assertions.assertEquals(2, server.httpHandlers().size());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    Assertions.assertEquals(200, connectTo(port, "test1?cancelNext=true").getResponseCode());
    Assertions.assertEquals(201, connectTo(port, "test1?cancelNext=false").getResponseCode());
  }

  @Test
  @Order(60)
  void testRequestResponseHeadersBody() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          Assertions.assertEquals("derklaro_was_here", context.request().header("derklaro_status"));
          Assertions.assertEquals(
            Document.newJsonDocument().append("test", true),
            DocumentFactory.json().parse(context.request().bodyStream()));

          context.response()
            .status(HttpResponseCode.CREATED)
            .header("derklaro_response_status", "derklaro_was_there")
            .body(Document.newJsonDocument().append("test", "passed").toString())
            .context()
            .closeAfter(true)
            .cancelNext();
        }
      });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    var connection = connectTo(port, "test", urlConnection -> {
      urlConnection.setRequestProperty("derklaro_status", "derklaro_was_here");
      urlConnection.setDoOutput(true);
    });

    try (var stream = connection.getOutputStream()) {
      Document.newJsonDocument().append("test", true).writeTo(stream);
    }

    Assertions.assertEquals(201, connection.getResponseCode());
    Assertions.assertEquals("derklaro_was_there", connection.getHeaderField("derklaro_response_status"));

    Assertions.assertEquals(
      Document.newJsonDocument().append("test", "passed"),
      DocumentFactory.json().parse(connection.getInputStream()));
  }

  @Test
  @Order(70)
  void testRequestResponseCookies() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          var cookie = context.cookie("request_cookie");
          Assertions.assertNotNull(cookie);

          Assertions.assertEquals("request_cookie", cookie.name());
          Assertions.assertEquals("request_value", cookie.value());

          context
            .response()
            .status(HttpResponseCode.OK)
            .context()
            .cancelNext(true)
            .closeAfter(true)
            .addCookie(new HttpCookie(
              "response_cookie",
              "response_value",
              "test1.com",
              "response_path",
              false,
              true,
              true,
              60_000
            ));
        }
      });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    var connection = connectTo(port, "test", urlConnection -> {
      var cookie = new DefaultHttpCookiePair("request_cookie", "request_value");
      urlConnection.setRequestProperty("Cookie", cookie.encoded().toString());
    });
    Assertions.assertEquals(200, connection.getResponseCode());

    var cookie = DefaultHttpSetCookie.parseSetCookie(connection.getHeaderField("Set-Cookie"), true);

    Assertions.assertEquals("response_cookie", cookie.name());
    Assertions.assertEquals("response_value", cookie.value());
    Assertions.assertEquals("test1.com", cookie.domain());
    Assertions.assertEquals("response_path", cookie.path());

    Assertions.assertTrue(cookie.isSecure());
    Assertions.assertFalse(cookie.isHttpOnly());
    Assertions.assertTrue(cookie.isWrapped());

    Assertions.assertEquals(60_000, cookie.maxAge());
  }

  @Test
  @Order(80)
  void testHandlersForDifferentPorts() throws Exception {
    var port = this.randomFreePort();
    var secondPort = this.randomFreePort(port);
    HttpServer server = new NettyHttpServer();

    // global handler
    server.registerHandler(
      "/global",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.response().status(HttpResponseCode.CREATED).context().cancelNext(true).closeAfter();
        }
      });
    // port specific handlers
    server.registerHandler(
      "/test1",
      port,
      HttpHandler.PRIORITY_NORMAL,
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter();
        }
      });
    server.registerHandler(
      "/test2",
      secondPort,
      HttpHandler.PRIORITY_NORMAL,
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter();
        }
      });

    Assertions.assertEquals(3, server.httpHandlers().size());
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());
    Assertions.assertDoesNotThrow(() -> server.addListener(secondPort).join());

    Assertions.assertEquals(201, connectTo(port, "global").getResponseCode());
    Assertions.assertEquals(201, connectTo(secondPort, "global").getResponseCode());

    Assertions.assertEquals(200, connectTo(port, "test1").getResponseCode());
    Assertions.assertEquals(404, connectTo(secondPort, "test1").getResponseCode());

    Assertions.assertEquals(404, connectTo(port, "test2").getResponseCode());
    Assertions.assertEquals(200, connectTo(secondPort, "test2").getResponseCode());
  }

  @Test
  @Order(90)
  @Timeout(20)
  void testCompressionRequest() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler("/test/plain", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {
        context.response().status(HttpResponseCode.OK).body("Hello World").context().cancelNext(true);
      }
    });
    server.registerHandler("/test/stream", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {
        context.response()
          .status(HttpResponseCode.OK)
          .body(new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8)))
          .context()
          .cancelNext(true);
      }
    });

    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    // without compression
    var connection = connectTo(port, "test/plain");
    Assertions.assertEquals(200, connection.getResponseCode());
    Assertions.assertEquals("Hello World", new String(connection.getInputStream().readAllBytes()));

    // with compression, plain, gzip
    connection = connectTo(port, "test/plain", con -> con.setRequestProperty("accept-encoding", "gzip"));
    Assertions.assertEquals(200, connection.getResponseCode());
    Assertions.assertEquals("Hello World", new String(new GZIPInputStream(connection.getInputStream()).readAllBytes()));

    // with compression, stream, gzip
    connection = connectTo(port, "test/stream", con -> con.setRequestProperty("accept-encoding", "gzip"));
    Assertions.assertEquals(200, connection.getResponseCode());
    Assertions.assertEquals("Hello World", new String(new GZIPInputStream(connection.getInputStream()).readAllBytes()));
  }

  @Test
  @Order(100)
  @Timeout(20)
  void testWebSocketHandling() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.upgrade().thenAccept(ch -> {
              ch.addListener((channel, type, content) -> {
                switch (type) {
                  case PING -> channel.sendWebSocketFrame(WebSocketFrameType.PONG, "response2");
                  case BINARY -> {
                    Assertions.assertArrayEquals(new byte[]{0, 5, 6}, content);
                    channel.close(1001, "Successful close");
                  }
                  default -> Assertions.fail("Unexpected frame type " + type);
                }
              });
              ch.sendWebSocketFrame(WebSocketFrameType.TEXT, "hello");
            }
          );
        }
      });
    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());

    var session = ClientManager.createClient().connectToServer(
      WebSocketClientEndpoint.class,
      URI.create(String.format("ws://127.0.0.1:%d/test", port)));

    while (session.isOpen()) {
      //noinspection BusyWait
      Thread.sleep(50);
    }
  }
}
