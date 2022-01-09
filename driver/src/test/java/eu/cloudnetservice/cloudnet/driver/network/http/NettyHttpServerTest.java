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

package eu.cloudnetservice.cloudnet.driver.network.http;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.function.ThrowableConsumer;
import eu.cloudnetservice.cloudnet.driver.network.NetworkTestCase;
import eu.cloudnetservice.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import eu.cloudnetservice.cloudnet.driver.network.netty.http.NettyHttpServer;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.glassfish.tyrus.client.ClientManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

@TestMethodOrder(OrderAnnotation.class)
public class NettyHttpServerTest extends NetworkTestCase {

  private static final List<String> SUPPORTED_METHODS = Arrays.asList(
    "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE");

  @Test
  @Order(0)
  void testHttpHandlerRegister() throws Exception {
    HttpServer server = new NettyHttpServer();

    server.registerHandler("/users/info", ($, $1) -> {
    }, ($, $1) -> {
    });
    server.registerHandler("users/{user}/info", ($, $1) -> {
    }, ($, $1) -> {
    });

    Assertions.assertEquals(4, server.httpHandlers().size());

    server.removeHandler(this.getClass().getClassLoader());
    Assertions.assertEquals(0, server.httpHandlers().size());
  }

  @Test
  @Order(10)
  void testDuplicateServerBind() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    Assertions.assertFalse(server.sslEnabled());
    Assertions.assertTrue(server.addListener(port));
    Assertions.assertFalse(server.addListener(port));

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
      ($, context) -> {
        Assertions.assertEquals(SUPPORTED_METHODS.get(handledTypes.getAndIncrement()), context.request().method());
        context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter();
      });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertTrue(server.addListener(port));

    for (var supportedMethod : SUPPORTED_METHODS) {
      var connection = this.connectTo(port, "test", con -> con.setRequestMethod(supportedMethod));

      connection.setRequestMethod(supportedMethod);

      connection.connect();
      Assertions.assertEquals(200, connection.getResponseCode());
    }
  }

  @Test
  @Order(30)
  void testRequestPathParameters() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test/{id}",
      ($, context) -> {
        Assertions.assertEquals("1234", context.request().pathParameters().get("id"));
        context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter();
      });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertTrue(server.addListener(port));

    Assertions.assertEquals(200, this.connectTo(port, "test/1234").getResponseCode());
    Assertions.assertEquals(404, this.connectTo(port, "test1234").getResponseCode());
  }

  @Test
  @Order(40)
  void testRequestQueryParameters() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test",
      ($, context) -> {
        Assertions.assertEquals("1234", Iterables.getFirst(context.request().queryParameters().get("id"), null));
        Assertions.assertIterableEquals(Arrays.asList("1", "2"), context.request().queryParameters().get("array"));
        context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter();
      });

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertTrue(server.addListener(port));

    Assertions.assertEquals(200, this.connectTo(port, "test?id=1234&array=1&array=2").getResponseCode());
    Assertions.assertEquals(404, this.connectTo(port, "test1234").getResponseCode());
  }

  @Test
  @Order(50)
  void testHandlerPriority() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test1",
      HttpHandler.PRIORITY_LOW,
      ($, context) -> {
        var cancelNext = Boolean.parseBoolean(
          Iterables.getFirst(context.request().queryParameters().get("cancelNext"), null));
        context.response().status(HttpResponseCode.OK).context().cancelNext(cancelNext);
      }
    );
    server.registerHandler(
      "/test1",
      HttpHandler.PRIORITY_HIGH,
      ($, context) -> context.response().status(HttpResponseCode.CREATED).context().closeAfter()
    );

    Assertions.assertEquals(2, server.httpHandlers().size());
    Assertions.assertTrue(server.addListener(port));

    Assertions.assertEquals(200, this.connectTo(port, "test1?cancelNext=true").getResponseCode());
    Assertions.assertEquals(201, this.connectTo(port, "test1?cancelNext=false").getResponseCode());
  }

  @Test
  @Order(60)
  void testRequestResponseHeadersBody() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test",
      ($, context) -> {
        Assertions.assertEquals("derklaro_was_here", context.request().header("derklaro_status"));
        Assertions.assertEquals(
          JsonDocument.newDocument("test", true),
          JsonDocument.newDocument(context.request().bodyStream()));

        context.response()
          .status(HttpResponseCode.CREATED)
          .header("derklaro_response_status", "derklaro_was_there")
          .body(JsonDocument.newDocument("test", "passed").toString().getBytes(StandardCharsets.UTF_8))
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    );

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertTrue(server.addListener(port));

    var connection = this.connectTo(port, "test", urlConnection -> {
      urlConnection.setRequestProperty("derklaro_status", "derklaro_was_here");
      urlConnection.setDoOutput(true);
    });

    try (var stream = connection.getOutputStream()) {
      JsonDocument.newDocument("test", true).write(stream);
    }

    Assertions.assertEquals(201, connection.getResponseCode());
    Assertions.assertEquals("derklaro_was_there", connection.getHeaderField("derklaro_response_status"));

    Assertions.assertEquals(
      JsonDocument.newDocument("test", "passed"),
      JsonDocument.newDocument(connection.getInputStream()));
  }

  @Test
  @Order(70)
  void testRequestResponseCookies() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test",
      ($, context) -> {
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
    );

    Assertions.assertEquals(1, server.httpHandlers().size());
    Assertions.assertTrue(server.addListener(port));

    var connection = this.connectTo(port, "test", urlConnection -> {
      Cookie cookie = new DefaultCookie("request_cookie", "request_value");
      urlConnection.setRequestProperty("Cookie", ClientCookieEncoder.LAX.encode(cookie));
    });
    Assertions.assertEquals(200, connection.getResponseCode());

    var cookie = ClientCookieDecoder.LAX.decode(connection.getHeaderField("Set-Cookie"));

    Assertions.assertEquals("response_cookie", cookie.name());
    Assertions.assertEquals("response_value", cookie.value());
    Assertions.assertEquals("test1.com", cookie.domain());
    Assertions.assertEquals("response_path", cookie.path());

    Assertions.assertTrue(cookie.isSecure());
    Assertions.assertFalse(cookie.isHttpOnly());
    Assertions.assertTrue(cookie.wrap());

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
      ($, context) -> context.response().status(HttpResponseCode.CREATED).context().cancelNext(true).closeAfter()
    );
    // port specific handlers
    server.registerHandler(
      "/test1",
      port,
      HttpHandler.PRIORITY_NORMAL,
      ($, context) -> context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter()
    );
    server.registerHandler(
      "/test2",
      secondPort,
      HttpHandler.PRIORITY_NORMAL,
      ($, context) -> context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter()
    );

    Assertions.assertEquals(3, server.httpHandlers().size());
    Assertions.assertTrue(server.addListener(port));
    Assertions.assertTrue(server.addListener(secondPort));

    Assertions.assertEquals(201, this.connectTo(port, "global").getResponseCode());
    Assertions.assertEquals(201, this.connectTo(secondPort, "global").getResponseCode());

    Assertions.assertEquals(200, this.connectTo(port, "test1").getResponseCode());
    Assertions.assertEquals(404, this.connectTo(secondPort, "test1").getResponseCode());

    Assertions.assertEquals(404, this.connectTo(port, "test2").getResponseCode());
    Assertions.assertEquals(200, this.connectTo(secondPort, "test2").getResponseCode());
  }

  @Test
  @Order(90)
  @Timeout(20)
  void testWebSocketHandling() throws Exception {
    var port = this.randomFreePort();
    HttpServer server = new NettyHttpServer();

    server.registerHandler(
      "/test",
      ($, context) -> context.upgrade().addListener((channel, type, content) -> {
        switch (type) {
          case TEXT -> {
            Assertions.assertEquals("request", new String(content, StandardCharsets.UTF_8));
            channel.sendWebSocketFrame(WebSocketFrameType.TEXT, "response");
          }
          case PING -> channel.sendWebSocketFrame(WebSocketFrameType.PONG, "response2");
          case BINARY -> {
            Assertions.assertArrayEquals(new byte[]{0, 5, 6}, content);
            channel.close(1001, "Successful close");
          }
          default -> Assertions.fail("Unexpected frame type " + type);
        }
      })
    );
    Assertions.assertTrue(server.addListener(port));

    var session = ClientManager.createClient().connectToServer(
      WebSocketClientEndpoint.class,
      URI.create(String.format("ws://127.0.0.1:%d/test", port)));

    while (session.isOpen()) {
      Thread.sleep(50);
    }
  }

  private HttpURLConnection connectTo(int port, String path) throws Exception {
    return this.connectTo(port, path, $ -> {
    });
  }

  private HttpURLConnection connectTo(
    int port,
    String path,
    ThrowableConsumer<HttpURLConnection, Exception> modifier
  ) throws Exception {
    var connection = (HttpURLConnection) this.httpUrl(port, path).openConnection();
    connection.setReadTimeout(5000);
    connection.setConnectTimeout(5000);

    modifier.accept(connection);
    return connection;
  }

  private URL httpUrl(int port, String path) throws Exception {
    return new URL(String.format("http://127.0.0.1:%d/%s", port, path == null ? "" : path));
  }
}
