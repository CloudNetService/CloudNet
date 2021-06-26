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

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.driver.network.netty.http.NettyHttpServer;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.junit.Assert;
import org.junit.Test;

public class NettySSLHttpServerTest {

  @Test
  public void testSslConfiguration() throws Exception {
    int port = NettyTestUtil.generateRandomPort();

    SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();

    try (IHttpServer httpServer = new NettyHttpServer(new SSLConfiguration(
      false,
      null,
      selfSignedCertificate.certificate().toPath(),
      selfSignedCertificate.privateKey().toPath()
    ))) {
      Assert.assertTrue(httpServer.isSslEnabled());
      Assert.assertTrue(httpServer.registerHandler("/test/power", (path, context) -> context.response()
        .header("Content-Type", "text/plain")
        .body("Data-Set")
        .statusCode(200)).addListener(port));

      HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);

      TrustManager[] trustManagers = new TrustManager[]{
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

          }

          @Override
          public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
          }

          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{selfSignedCertificate.cert()};
          }
        }
      };

      SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustManagers, new SecureRandom());

      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

      HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://127.0.0.1:" + port + "/test/power")
        .openConnection();
      httpURLConnection.connect();

      Assert.assertEquals(HttpResponseCode.HTTP_OK, httpURLConnection.getResponseCode());

      try (InputStream inputStream = httpURLConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(
          new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        Assert.assertEquals("Data-Set", bufferedReader.readLine());
      }

      httpURLConnection.disconnect();
    }
  }
}
