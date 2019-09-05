package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

public final class V1HttpHandlerAuthentication extends V1HttpHandler {

    public V1HttpHandlerAuthentication() {
        super(null);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) throws Exception {
        this.sendOptions(context, "OPTIONS, GET");
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        if (HTTP_SESSION.auth(context)) {
            if (context.request().queryParameters().containsKey("redirect")) {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_MOVED_TEMP)
                        .header("Location", context.request().queryParameters().get("redirect").iterator().next())
                        .context()
                        .closeAfter(true)
                        .cancelNext()
                ;
            } else {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_OK)
                        .header("Content-Type", "application/json")
                        .body(new JsonDocument("success", true).append("userUniqueId", HTTP_SESSION.getUser(context).getUniqueId()).toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext()
                ;
            }
        } else {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"CloudNet-REST-v1\"")
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        }
    }
}