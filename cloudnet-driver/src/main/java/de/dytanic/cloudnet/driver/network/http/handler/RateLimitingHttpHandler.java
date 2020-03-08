package de.dytanic.cloudnet.driver.network.http.handler;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RateLimitingHttpHandler implements IHttpHandler {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private static final byte[] TOO_MANY_REQUESTS = JsonDocument.newDocument().append("error", "rate limit exceeded").toPrettyJson().getBytes(StandardCharsets.UTF_8);

    private Map<String, Integer> ipToRequestCount = new ConcurrentHashMap<>();

    private int maxRequests;
    private long nextRefresh;

    public RateLimitingHttpHandler(int maxRequests, TimeUnit timeUnit) {
        this.maxRequests = maxRequests;

        EXECUTOR_SERVICE.execute(() -> {
            while (!Thread.interrupted()) {
                long time = timeUnit.toMillis(1);
                this.nextRefresh = System.currentTimeMillis() + time;
                try {
                    Thread.sleep(time);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
                this.ipToRequestCount.clear();
            }
        });
    }

    @Override
    public void handle(String path, IHttpContext context) throws Exception {
        String ip = context.channel().clientAddress().getHost();

        this.ipToRequestCount.putIfAbsent(ip, 0);

        this.ipToRequestCount.compute(ip, (key, count) -> {
            if (count == null) {
                count = 1;
            }
            if (count < maxRequests) {
                return ++count;
            }
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_TOO_MANY_REQUESTS)
                    .body(TOO_MANY_REQUESTS)
                    .header("X-RateLimit-End", String.valueOf(this.nextRefresh))
                    .context()
                    .cancelNext();
            return count;
        });
    }
}
