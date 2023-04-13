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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.NetworkTestCase;
import eu.cloudnetservice.driver.network.http.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.Optional;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestHeader;
import eu.cloudnetservice.driver.network.http.annotation.RequestPath;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.driver.network.http.annotation.RequestQueryParam;
import eu.cloudnetservice.driver.network.netty.http.NettyHttpServer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NettyHttpAnnotatedHandlerTest extends NetworkTestCase {

  private int serverPort;
  private HttpServer httpServer;

  @BeforeEach
  void initHttpServer() {
    this.serverPort = randomFreePort();

    this.httpServer = new NettyHttpServer();
    this.httpServer.addListener(this.serverPort).join();
    this.httpServer.annotationParser().parseAndRegister(new AnnotatedHandler());
  }

  @AfterEach
  void teardownHttpServer() throws Exception {
    this.httpServer.close();
  }

  @Test
  void testHandlerCalledCorrectlyWithOptional() throws Exception {
    var client = HttpClient.newHttpClient();

    var handlerRequestUri = UriBuilder.create().port(this.serverPort).path("test", "hello", "1").build();
    var handlerRequest = HttpRequest.newBuilder(handlerRequestUri).build();
    var response = client.send(handlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(200, response.statusCode());

    var firstHeaderValue = response.headers().firstValue("Test").orElse(null);
    Assertions.assertNotNull(firstHeaderValue);
    Assertions.assertEquals("Handler1", firstHeaderValue);
  }

  @Test
  void testHandlerCalledCorrectlyWithoutOptional() throws Exception {
    var client = HttpClient.newHttpClient();

    var handlerRequestUri = UriBuilder.create()
      .port(this.serverPort)
      .path("test", "hello", "2")
      .addQueryParameter("testValue", "test")
      .build();
    var handlerRequest = HttpRequest.newBuilder(handlerRequestUri).build();
    var response = client.send(handlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(200, response.statusCode());

    var firstHeaderValue = response.headers().firstValue("Test").orElse(null);
    Assertions.assertNotNull(firstHeaderValue);
    Assertions.assertEquals("Handler2", firstHeaderValue);
  }

  @Test
  void testRespondsWithBadRequestOnInvalidRequest() throws Exception {
    var client = HttpClient.newHttpClient();

    var handlerUri = UriBuilder.create()
      .port(this.serverPort)
      .path("test", "all", "2")
      .addQueryParameter("testValue", "world")
      .build();
    var handlerRequest = HttpRequest.newBuilder(handlerUri).build();
    var handlerResponse = client.send(handlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(400, handlerResponse.statusCode());
  }

  @Test
  void testHandlerGetsCalledOnFullRequest() throws Exception {
    var client = HttpClient.newHttpClient();

    var handlerUri = UriBuilder.create()
      .port(this.serverPort)
      .path("test", "all", "2")
      .addQueryParameter("testValue", "present")
      .addQueryParameter("testValue3", "present")
      .addQueryParameter("testValue4", "hello")
      .addQueryParameter("testValue4", "world")
      .build();
    var handlerRequest = HttpRequest.newBuilder(handlerUri).header("test", "present").build();
    var handlerResponse = client.send(handlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(201, handlerResponse.statusCode());

    var firstHeaderValue = handlerResponse.headers().firstValue("Test").orElse(null);
    Assertions.assertNotNull(firstHeaderValue);
    Assertions.assertEquals("Response2", firstHeaderValue);
  }

  @Test
  void testHandlerRejectRequestWithInvalidMethod() throws Exception {
    var client = HttpClient.newHttpClient();

    var handlerUri = UriBuilder.create().port(this.serverPort).path("test", "body").build();
    var handlerRequest = HttpRequest.newBuilder(handlerUri).GET().build();
    var handlerResponse = client.send(handlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(404, handlerResponse.statusCode());
  }

  @Test
  void testHandlerCalledCorrectlyWithBody() throws Exception {
    var client = HttpClient.newHttpClient();

    var body = Document.newJsonDocument().append("test", "hello world!").serializeToString();
    var handlerUri = UriBuilder.create().port(this.serverPort).path("test", "body").build();
    var handlerRequest = HttpRequest.newBuilder(handlerUri)
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build();
    var handlerResponse = client.send(handlerRequest, HttpResponse.BodyHandlers.discarding());
    Assertions.assertEquals(202, handlerResponse.statusCode());
  }

  public static final class AnnotatedHandler {

    @HttpRequestHandler(paths = "/test/hello/{id}", priority = 0)
    public void handleTestHello(
      HttpContext context,
      @RequestPathParam("id") String id,
      @Optional @FirstRequestQueryParam("testValue") String testValue
    ) {
      Assertions.assertTrue((id.equals("1") && testValue == null) || (id.equals("2") && testValue.equals("test")));
      context.cancelNext(true).response().status(HttpResponseCode.OK).header("Test", "Handler" + id);
    }

    @HttpRequestHandler(paths = "/test/all/{id}", priority = 0)
    public void handleAll(
      HttpContext context,
      @RequestPath String path,
      @RequestPathParam("id") String id,
      @RequestHeader(value = "test", def = "absent") String test,
      @Optional @RequestHeader(value = "test2", def = "absent") String test2,
      @FirstRequestQueryParam(value = "testValue", def = "absent") String testValue,
      @Optional @FirstRequestQueryParam(value = "testValue2", def = "absent") String testValue2,
      @Optional @FirstRequestQueryParam(value = "testValue3", def = "absent") String testValue3,
      @RequestQueryParam("testValue4") List<String> testValue4,
      @Optional @RequestQueryParam("testValue5") Collection<String> testValue5,
      @Optional @RequestQueryParam(value = "testValue6", nullWhenAbsent = true) Collection<String> testValue6
    ) {
      Assertions.assertEquals("/test/all/2", path);
      Assertions.assertEquals("2", id);
      Assertions.assertEquals("present", test);
      Assertions.assertEquals("absent", test2);
      Assertions.assertEquals("present", testValue);
      Assertions.assertEquals("absent", testValue2);
      Assertions.assertEquals("present", testValue3);
      Assertions.assertLinesMatch(List.of("hello", "world"), testValue4);
      Assertions.assertTrue(testValue5 != null && testValue5.isEmpty());
      Assertions.assertNull(testValue6);

      context.cancelNext(true).response().status(HttpResponseCode.CREATED).header("Test", "Response" + id);
    }

    @HttpRequestHandler(paths = "/test/body", methods = "POST")
    public void handleBody(HttpContext context, @RequestBody String body, @RequestBody Document bodyDoc) {
      Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"));
      Assertions.assertEquals("hello world!", bodyDoc.getString("test"));
      context.response().status(HttpResponseCode.ACCEPTED);
    }
  }
}
