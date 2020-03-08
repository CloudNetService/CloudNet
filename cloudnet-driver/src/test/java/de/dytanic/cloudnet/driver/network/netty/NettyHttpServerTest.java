package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NettyHttpServerTest {

    private static final String TEST_STRING = "Response Text", TEST_STRING_2 = "Bernd", TEST_STRING_2_MESSAGE = "Eine Test Nachricht";
    private static final String RATE_LIMIT_STRING = JsonDocument.newDocument().append("error", "rate limit exceeded").toPrettyJson();

    @Test
    public void testHttpServerWithParameters() throws Exception {
        int port = NettyTestUtil.generateRandomPort();

        IHttpServer httpServer = new NettyHttpServer();

        Assert.assertNotNull(httpServer.rateLimit("/person/{id}/{name}/info", TimeUnit.MINUTES, 2));

        Assert.assertNotNull(httpServer.registerHandler("/person/{id}/{name}/info", (path, context) -> {
            if (context.request().pathParameters().containsKey("id") && context.request().pathParameters().containsKey("name") &&
                    context.request().pathParameters().get("id").equals("64") && context.request().pathParameters().get("name").equals("Albert") &&
                    context.request().method().toUpperCase().equals("GET")) {
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

        Assert.assertEquals(2, httpServer.getHttpHandlers().size());
        Assert.assertTrue(httpServer.addListener(port));

        Assert.assertEquals(TEST_STRING, this.request(port, "/person/64/Albert/info", httpURLConnection -> {
            Assert.assertEquals(200, httpURLConnection.getResponseCode());
            Assert.assertEquals("true", httpURLConnection.getHeaderField("Custom-Header"));
        }));
        Assert.assertEquals(TEST_STRING, this.request(port, "/person/64/Albert/info", httpURLConnection -> {
            Assert.assertEquals(200, httpURLConnection.getResponseCode());
            Assert.assertEquals("true", httpURLConnection.getHeaderField("Custom-Header"));
        }));
        Assert.assertEquals(RATE_LIMIT_STRING, this.request(port, "/person/64/Albert/info", httpURLConnection -> {
            Assert.assertEquals(429, httpURLConnection.getResponseCode());
            Assert.assertTrue(httpURLConnection.getHeaderFields().containsKey("X-RateLimit-End"));
        }));

        Assert.assertEquals(0, httpServer.clearHandlers().getHttpHandlers().size());

        httpServer.close();
    }

    private String request(int port, String path, URLConnectionTester tester) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("http://localhost:" + port + path).openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setDoOutput(false);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.connect();

        tester.test(httpURLConnection);

        StringBuilder response = new StringBuilder();
        try (InputStream inputStream = httpURLConnection.getResponseCode() == 200 ? httpURLConnection.getInputStream() : httpURLConnection.getErrorStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line).append("\n");
            }
        }

        httpURLConnection.disconnect();

        return response.length() == 0 ? "" : response.substring(0, response.length() - 1);
    }

    @Test
    public void testHttpServerWithWildCard() throws Exception {
        int port = NettyTestUtil.generateRandomPort();

        IHttpServer httpServer = new NettyHttpServer();

        Assert.assertNotNull(httpServer.registerHandler("/person/*/test", (path, context) -> {
            if (context.request().method().toUpperCase().equals("POST")) {
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

        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("http://localhost:" + port + "/person/" + TEST_STRING_2 + "/test").openConnection();
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

        try (InputStream inputStream = httpURLConnection.getInputStream(); BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                inputStream
        ))) {
            Assert.assertEquals(TEST_STRING_2_MESSAGE, bufferedReader.readLine());
        }

        Assert.assertEquals(0, httpServer.clearHandlers().getHttpHandlers().size());

        httpURLConnection.disconnect();
        httpServer.close();
    }

    private static interface URLConnectionTester {
        void test(HttpURLConnection connection) throws IOException;
    }

}