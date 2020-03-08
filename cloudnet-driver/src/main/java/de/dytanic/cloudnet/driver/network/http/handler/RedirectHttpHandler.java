package de.dytanic.cloudnet.driver.network.http.handler;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.MethodHttpHandlerAdapter;

import java.util.Map;

public class RedirectHttpHandler extends MethodHttpHandlerAdapter {

    private String redirectResponse;

    public RedirectHttpHandler(String redirectResponse) {
        this.redirectResponse = redirectResponse;
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        String response = this.redirectResponse;
        for (Map.Entry<String, String> entry : context.request().pathParameters().entrySet()) {
            response = response.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        context.response()
                .statusCode(HttpResponseCode.HTTP_MOVED_PERM)
                .header("Location", response)
                .body(" ")
                .context()
                .cancelNext();
    }
}
