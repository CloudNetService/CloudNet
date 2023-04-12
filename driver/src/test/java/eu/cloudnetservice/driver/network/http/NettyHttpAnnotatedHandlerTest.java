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

import static eu.cloudnetservice.driver.network.http.NettyHttpServerTest.connectTo;

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
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NettyHttpAnnotatedHandlerTest extends NetworkTestCase {

  @Test
  void testHandlerCalledCorrectlyWithOptional() throws Exception {
    var port = this.randomFreePort();
    var server = new NettyHttpServer();

    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());
    Assertions.assertDoesNotThrow(() -> server.annotationParser().parseAndRegister(new AnnotatedHandler()));

    var connection = Assertions.assertDoesNotThrow(() -> connectTo(port, "test/hello/1"));
    Assertions.assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
    Assertions.assertEquals("Handler1", connection.getHeaderField("Test"));

    var connection2 = Assertions.assertDoesNotThrow(() -> connectTo(port, "test/hello/2?testValue=test"));
    Assertions.assertEquals(HttpURLConnection.HTTP_OK, connection2.getResponseCode());
    Assertions.assertEquals("Handler2", connection2.getHeaderField("Test"));
  }

  @Test
  void testHandlerCalledCorrectlyWithMiscAnnotations() throws Exception {
    var port = this.randomFreePort();
    var server = new NettyHttpServer();

    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());
    Assertions.assertDoesNotThrow(() -> server.annotationParser().parseAndRegister(new AnnotatedHandler()));

    var connection = Assertions.assertDoesNotThrow(() -> connectTo(port, "test/value/2"));
    Assertions.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, connection.getResponseCode());

    var connection2 = Assertions.assertDoesNotThrow(() -> connectTo(port, "test/all/2"));
    Assertions.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, connection2.getResponseCode());

    var connection3 = Assertions.assertDoesNotThrow(() -> connectTo(
      port,
      "test/all/2?testValue=present&testValue3=present&testValue4=hello&testValue4=world",
      con -> con.setRequestProperty("test", "present")));
    Assertions.assertEquals(HttpURLConnection.HTTP_CREATED, connection3.getResponseCode());
    Assertions.assertEquals("Response2", connection3.getHeaderField("Test"));
  }

  @Test
  void testHandlerCalledCorrectlyWithBody() throws Exception {
    var port = this.randomFreePort();
    var server = new NettyHttpServer();

    Assertions.assertDoesNotThrow(() -> server.addListener(port).join());
    Assertions.assertDoesNotThrow(() -> server.annotationParser().parseAndRegister(new AnnotatedHandler()));

    var connection = Assertions.assertDoesNotThrow(() -> connectTo(port, "test/body"));
    Assertions.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, connection.getResponseCode());

    var connection2 = Assertions.assertDoesNotThrow(() -> connectTo(port, "test/body", con -> {
      con.setDoOutput(true);
      con.setRequestMethod("POST");
    }));
    try (var out = connection2.getOutputStream()) {
      out.write(Document.newJsonDocument().append("test", "hello world!").toString().getBytes(StandardCharsets.UTF_8));
      out.flush();
    }

    Assertions.assertEquals(HttpURLConnection.HTTP_ACCEPTED, connection2.getResponseCode());
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
