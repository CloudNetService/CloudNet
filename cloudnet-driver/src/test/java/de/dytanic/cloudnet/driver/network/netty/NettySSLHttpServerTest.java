package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.HttpHandler;
import de.dytanic.cloudnet.driver.network.http.HttpServer;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class NettySSLHttpServerTest {

    @Test
    public void testSslConfiguration() throws Exception {
        int port = NettyTestUtil.generateRandomPort();

        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();

        try (HttpServer httpServer = new NettyHttpServer(new SSLConfiguration(
                false,
                null,
                selfSignedCertificate.certificate(),
                selfSignedCertificate.privateKey()
        ))) {
            Assert.assertTrue(httpServer.isSslEnabled());
            Assert.assertTrue(httpServer.registerHandler("/test/power", (HttpHandler) (path, context) -> context.response()
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

            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://127.0.0.1:" + port + "/test/power").openConnection();
            httpURLConnection.connect();

            Assert.assertEquals(HttpResponseCode.HTTP_OK, httpURLConnection.getResponseCode());

            try (InputStream inputStream = httpURLConnection.getInputStream();
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                Assert.assertEquals("Data-Set", bufferedReader.readLine());
            }

            httpURLConnection.disconnect();
        }
    }
}