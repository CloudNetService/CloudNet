package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

public final class V1SecurityProtectionHttpHandler extends V1HttpHandler {

    public V1SecurityProtectionHttpHandler() {
        super(null);
    }

    @Override
    public void handle(String path, IHttpContext context) throws Exception {
        if (path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/logout")) {
            return;
        }

        if (!HTTP_SESSION.isAuthorized(context)) {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_MOVED_TEMP)
                    .header("Location", "/api/v1/auth?redirect=" + context.request().uri())
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        }
    }
}