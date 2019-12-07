package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

public final class V1HttpHandlerPing extends V1HttpHandler {

    public V1HttpHandlerPing(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, HttpContext context) {
        this.sendOptions(context, "OPTIONS, GET");
    }

    @Override
    public void handleGet(String path, HttpContext context) {
        context
                .response()
                .header("Content-Type", "application/json")
                .body(new JsonDocument("success", true).toByteArray())
                .statusCode(200)
                .context()
                .closeAfter(true)
                .cancelNext();
    }
}