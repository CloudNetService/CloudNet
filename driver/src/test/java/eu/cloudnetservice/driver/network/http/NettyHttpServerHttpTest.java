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
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.network.NetworkTestCase;
import eu.cloudnetservice.driver.network.netty.http.NettyHttpServer;
import io.netty5.handler.codec.http.headers.DefaultHttpCookiePair;
import io.netty5.handler.codec.http.headers.DefaultHttpSetCookie;
import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@SuppressWarnings("NullableProblems")
public class NettyHttpServerHttpTest extends NetworkTestCase {

  private static final List<String> SUPPORTED_METHODS = Arrays.asList(
    "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE");

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
  void testHttpHandlerRegister() {
    this.httpServer.registerHandler("/users/info", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {

      }
    });
    this.httpServer.registerHandler("users/{user}/info", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {

      }
    });

    Assertions.assertEquals(2, this.httpServer.httpHandlers().size());

    this.httpServer.removeHandler(this.getClass().getClassLoader());
    Assertions.assertEquals(0, this.httpServer.httpHandlers().size());
  }

  @Test
  @Timeout(20)
  void testHttpRequestMethods() throws Exception {
    var handledTypes = new AtomicInteger();
    this.httpServer.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          Assertions.assertEquals(SUPPORTED_METHODS.get(handledTypes.getAndIncrement()), context.request().method());
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter(true);
        }
      });
    Assertions.assertEquals(1, this.httpServer.httpHandlers().size());

    var client = HttpClient.newHttpClient();
    var requestUri = UriBuilder.create().port(this.serverPort).path("test").build();
    for (var supportedMethod : SUPPORTED_METHODS) {
      var request = HttpRequest.newBuilder(requestUri)
        .method(supportedMethod, HttpRequest.BodyPublishers.noBody())
        .build();
      var serverResponse = client.send(request, HttpResponse.BodyHandlers.discarding());
      Assertions.assertEquals(200, serverResponse.statusCode());
    }
  }

  @Test
  @Timeout(20)
  void testRootHandler() throws Exception {
    var client = HttpClient.newHttpClient();

    this.httpServer.registerHandler("/", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {
        context.response().status(HttpResponseCode.OK).body("Hello World!").context().cancelNext(true);
      }
    });
    Assertions.assertEquals(1, this.httpServer.httpHandlers().size());

    var rootRequestUri = UriBuilder.create().port(this.serverPort).build();
    var rootServerRequest = HttpRequest.newBuilder(rootRequestUri).build();
    var rootServerResponse = client.send(rootServerRequest, HttpResponse.BodyHandlers.ofString());
    Assertions.assertEquals(200, rootServerResponse.statusCode());
    Assertions.assertEquals("Hello World!", rootServerResponse.body());

    var randomRequestUri = UriBuilder.create().port(this.serverPort).path("testing").build();
    var randomServerRequest = HttpRequest.newBuilder(randomRequestUri).build();
    var randomServerResponse = client.send(randomServerRequest, HttpResponse.BodyHandlers.ofString());
    Assertions.assertEquals(404, randomServerResponse.statusCode());
    Assertions.assertEquals("Resource not found!", randomServerResponse.body());
  }

  @Test
  @Timeout(20)
  void testRequestPathParameters() throws Exception {
    var client = HttpClient.newHttpClient();

    this.httpServer.registerHandler(
      "/test/{id}",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          Assertions.assertEquals("1234", context.request().pathParameters().get("id"));
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter(true);
        }
      });
    Assertions.assertEquals(1, this.httpServer.httpHandlers().size());

    var testHandlerUri = UriBuilder.create().port(this.serverPort).path("test", "1234").build();
    var testHandlerRequest = HttpRequest.newBuilder(testHandlerUri).build();
    var testServerResponse = client.send(testHandlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(200, testServerResponse.statusCode());

    var randomRequestUri = UriBuilder.create().port(this.serverPort).path("test1234").build();
    var randomServerRequest = HttpRequest.newBuilder(randomRequestUri).build();
    var randomServerResponse = client.send(randomServerRequest, HttpResponse.BodyHandlers.ofString());
    Assertions.assertEquals(404, randomServerResponse.statusCode());
    Assertions.assertEquals("Resource not found!", randomServerResponse.body());
  }

  @Test
  @Timeout(20)
  void testRequestQueryParameters() throws Exception {
    var client = HttpClient.newHttpClient();

    this.httpServer.registerHandler(
      "/test",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          Assertions.assertEquals("1234", Iterables.getFirst(context.request().queryParameters().get("id"), null));
          Assertions.assertIterableEquals(Arrays.asList("1", "2"), context.request().queryParameters().get("array"));
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter(true);
        }
      });
    Assertions.assertEquals(1, this.httpServer.httpHandlers().size());

    var testHandlerUri = UriBuilder.create()
      .port(this.serverPort)
      .path("test")
      .addQueryParameter("id", "1234")
      .addQueryParameter("array", "1")
      .addQueryParameter("array", "2")
      .build();
    var testHandlerRequest = HttpRequest.newBuilder(testHandlerUri).build();
    var testServerResponse = client.send(testHandlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(200, testServerResponse.statusCode());

    var randomRequestUri = UriBuilder.create().port(this.serverPort).path("test1234").build();
    var randomServerRequest = HttpRequest.newBuilder(randomRequestUri).build();
    var randomServerResponse = client.send(randomServerRequest, HttpResponse.BodyHandlers.ofString());
    Assertions.assertEquals(404, randomServerResponse.statusCode());
    Assertions.assertEquals("Resource not found!", randomServerResponse.body());
  }

  @Test
  @Timeout(20)
  void testHandlerPriority() throws Exception {
    var client = HttpClient.newHttpClient();

    this.httpServer.registerHandler(
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
    this.httpServer.registerHandler(
      "/test1",
      HttpHandler.PRIORITY_HIGH,
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.response().status(HttpResponseCode.CREATED).context().closeAfter(true);
        }
      });
    Assertions.assertEquals(2, this.httpServer.httpHandlers().size());

    var cancelNextUri = UriBuilder.create()
      .port(this.serverPort)
      .path("test1")
      .addQueryParameter("cancelNext", "true")
      .build();
    var cancelNextHandlerRequest = HttpRequest.newBuilder(cancelNextUri).build();
    var cancelNextServerResponse = client.send(cancelNextHandlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(200, cancelNextServerResponse.statusCode());

    var noCancelNextUri = UriBuilder.create()
      .port(this.serverPort)
      .path("test1")
      .addQueryParameter("cancelNext", "false")
      .build();
    var noCancelNextHandlerRequest = HttpRequest.newBuilder(noCancelNextUri).build();
    var noCancelNextServerResponse = client.send(noCancelNextHandlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(201, noCancelNextServerResponse.statusCode());
  }

  @Test
  @Timeout(20)
  void testRequestResponseHeadersBody() throws Exception {
    var client = HttpClient.newHttpClient();

    this.httpServer.registerHandler(
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
            .cancelNext(true);
        }
      });
    Assertions.assertEquals(1, this.httpServer.httpHandlers().size());

    var requestBody = Document.newJsonDocument().append("test", true).toString();
    var requestUri = UriBuilder.create().port(this.serverPort).path("test").build();
    var request = HttpRequest.newBuilder(requestUri).POST(HttpRequest.BodyPublishers.ofString(requestBody))
      .header("derklaro_status", "derklaro_was_here")
      .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

    Assertions.assertEquals(201, response.statusCode());
    Assertions.assertEquals(
      "derklaro_was_there",
      response.headers().firstValue("derklaro_response_status").orElse(null));

    var responseDocument = DocumentFactory.json().parse(response.body());
    Assertions.assertEquals("passed", responseDocument.getString("test"));
  }

  @Test
  @Timeout(20)
  void testRequestResponseCookies() throws Exception {
    var client = HttpClient.newHttpClient();

    this.httpServer.registerHandler(
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
    Assertions.assertEquals(1, this.httpServer.httpHandlers().size());

    var cookie = new DefaultHttpCookiePair("request_cookie", "request_value").encoded().toString();
    var requestUri = UriBuilder.create().port(this.serverPort).path("test").build();
    var request = HttpRequest.newBuilder(requestUri).header("Cookie", cookie).build();
    var response = client.send(request, HttpResponse.BodyHandlers.discarding());

    Assertions.assertEquals(200, response.statusCode());

    var setCookieHeader = response.headers().firstValue("Set-Cookie").orElse(null);
    Assertions.assertNotNull(setCookieHeader);

    var responseCookie = DefaultHttpSetCookie.parseSetCookie(setCookieHeader, true);
    Assertions.assertEquals("response_cookie", responseCookie.name());
    Assertions.assertEquals("response_value", responseCookie.value());
    Assertions.assertEquals("test1.com", responseCookie.domain());
    Assertions.assertEquals("response_path", responseCookie.path());
    Assertions.assertTrue(responseCookie.isSecure());
    Assertions.assertFalse(responseCookie.isHttpOnly());
    Assertions.assertTrue(responseCookie.isWrapped());
    Assertions.assertEquals(60_000, responseCookie.maxAge());
  }

  @Test
  @Timeout(20)
  void testHandlersForDifferentPorts() throws Exception {
    var client = HttpClient.newHttpClient();

    // bind to the second port, ignore the port we're already using
    var secondPort = randomFreePort(this.serverPort);
    Assertions.assertDoesNotThrow(() -> this.httpServer.addListener(secondPort).join());

    // global handler
    this.httpServer.registerHandler(
      "/global",
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.response().status(HttpResponseCode.CREATED).context().cancelNext(true).closeAfter(true);
        }
      });
    // port specific handlers
    this.httpServer.registerHandler(
      "/test",
      this.serverPort,
      HttpHandler.PRIORITY_NORMAL,
      new HttpHandler() {
        @Override
        public void handle(String path, HttpContext context) {
          context.response().status(HttpResponseCode.OK).context().cancelNext(true).closeAfter(true);
        }
      });
    Assertions.assertEquals(2, this.httpServer.httpHandlers().size());

    var globalFirstPortUri = UriBuilder.create().port(this.serverPort).path("global").build();
    var globalFirstPortRequest = HttpRequest.newBuilder(globalFirstPortUri).build();
    var globalFirstResponse = client.send(globalFirstPortRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(201, globalFirstResponse.statusCode());

    var globalSecondPortUri = UriBuilder.create().port(secondPort).path("global").build();
    var globalSecondPortRequest = HttpRequest.newBuilder(globalSecondPortUri).build();
    var globalSecondResponse = client.send(globalSecondPortRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(201, globalSecondResponse.statusCode());

    var testFirstPortUri = UriBuilder.create().port(this.serverPort).path("test").build();
    var testFirstPortRequest = HttpRequest.newBuilder(testFirstPortUri).build();
    var testFirstPortResponse = client.send(testFirstPortRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(200, testFirstPortResponse.statusCode());

    var testSecondPortUri = UriBuilder.create().port(secondPort).path("test").build();
    var testSecondPortRequest = HttpRequest.newBuilder(testSecondPortUri).build();
    var testSecondPortResponse = client.send(testSecondPortRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(404, testSecondPortResponse.statusCode());
  }

  @Test
  @Timeout(20)
  void testCompressionRequest() throws Exception {
    var client = HttpClient.newHttpClient();

    this.httpServer.registerHandler("/test/stream", new HttpHandler() {
      @Override
      public void handle(String path, HttpContext context) {
        context.response()
          .status(HttpResponseCode.OK)
          .body(new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8)))
          .context()
          .cancelNext(true);
      }
    });
    Assertions.assertEquals(1, this.httpServer.httpHandlers().size());

    // without compression
    var streamHandlerUri = UriBuilder.create().port(this.serverPort).path("test", "stream").build();
    var streamHandlerRequest = HttpRequest.newBuilder(streamHandlerUri).build();
    var streamHandlerResponse = client.send(streamHandlerRequest, HttpResponse.BodyHandlers.ofInputStream());
    Assertions.assertEquals(200, streamHandlerResponse.statusCode());
    Assertions.assertEquals(
      "Hello World",
      new String(streamHandlerResponse.body().readAllBytes(), StandardCharsets.UTF_8));

    // with compression, gzip
    var gzipStreamHandlerRequest = HttpRequest.newBuilder(streamHandlerUri).header("accept-encoding", "gzip").build();
    var gzipStreamHandlerResponse = client.send(gzipStreamHandlerRequest, HttpResponse.BodyHandlers.ofInputStream());
    Assertions.assertEquals(200, gzipStreamHandlerResponse.statusCode());
    Assertions.assertEquals(
      "Hello World",
      new String(new GZIPInputStream(gzipStreamHandlerResponse.body()).readAllBytes(), StandardCharsets.UTF_8));
  }
}
