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

package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.netty.http.NettyHttpServer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;

public class NettyHttpServerTest {

  private static final String TEST_STRING = "Response Text";
  private static final String TEST_STRING_2 = "Bernd";
  private static final String TEST_STRING_2_MESSAGE = "Eine Test Nachricht";

  @Test
  public void testHttpServerWithParameters() throws Exception {
    int port = NettyTestUtil.generateRandomPort();

    IHttpServer httpServer = new NettyHttpServer();

    Assert.assertNotNull(httpServer.registerHandler("/person/{id}/{name}/info", (path, context) -> {
      if (context.request().pathParameters().containsKey("id") && context.request().pathParameters().containsKey("name")
        &&
        context.request().pathParameters().get("id").equals("64") && context.request().pathParameters().get("name")
        .equals("Albert") &&
        context.request().method().equalsIgnoreCase("GET")) {
        context
          .response()
          .header("Content-Type", "text/plain")
          .header("Custom-Header", "true")
          .body(TEST_STRING)
          .statusCode(200)
          .context()
          .cancelNext()
        ;
      } else {
        context.response()
          .statusCode(404)
          .context()
          .cancelNext()
        ;
      }
    }));

    Assert.assertEquals(1, httpServer.getHttpHandlers().size());
    Assert.assertTrue(httpServer.addListener(port));

    HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(
      "http://localhost:" + port + "/person/64/Albert/info").openConnection();
    httpURLConnection.setRequestMethod("GET");
    httpURLConnection.setDoOutput(false);
    httpURLConnection.setUseCaches(false);
    httpURLConnection.connect();

    Assert.assertEquals(200, httpURLConnection.getResponseCode());
    Assert.assertEquals("true", httpURLConnection.getHeaderField("Custom-Header"));

    try (InputStream inputStream = httpURLConnection
      .getInputStream(); BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
      inputStream
    ))) {
      Assert.assertEquals(TEST_STRING, bufferedReader.readLine());
    }

    Assert.assertEquals(0, httpServer.clearHandlers().getHttpHandlers().size());

    httpURLConnection.disconnect();
    httpServer.close();
  }

  @Test
  public void testHttpServerWithWildCard() throws Exception {
    int port = NettyTestUtil.generateRandomPort();

    IHttpServer httpServer = new NettyHttpServer();

    Assert.assertNotNull(httpServer.registerHandler("/person/*/test", (path, context) -> {
      if (context.request().method().equalsIgnoreCase("POST")) {
        context
          .response()
          .header("Content-Type", "text/plain")
          .header("Request-Text-Example", path.split("/")[2])
          .body(context.request().body())
          .statusCode(200)
          .context()
          .cancelNext()
        ;
      }
    }));

    Assert.assertEquals(1, httpServer.getHttpHandlers().size());
    Assert.assertTrue(httpServer.addListener(port));

    HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(
      "http://localhost:" + port + "/person/" + TEST_STRING_2 + "/test").openConnection();
    httpURLConnection.setRequestMethod("POST");
    httpURLConnection.setDoOutput(true);
    httpURLConnection.setUseCaches(false);
    httpURLConnection.connect();

    try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
      outputStream.write(TEST_STRING_2_MESSAGE.getBytes());
      outputStream.flush();
    }

    Assert.assertEquals(200, httpURLConnection.getResponseCode());
    Assert.assertEquals(TEST_STRING_2, httpURLConnection.getHeaderField("Request-Text-Example"));

    try (InputStream inputStream = httpURLConnection
      .getInputStream(); BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
      inputStream
    ))) {
      Assert.assertEquals(TEST_STRING_2_MESSAGE, bufferedReader.readLine());
    }

    Assert.assertEquals(0, httpServer.clearHandlers().getHttpHandlers().size());

    httpURLConnection.disconnect();
    httpServer.close();
  }
}
