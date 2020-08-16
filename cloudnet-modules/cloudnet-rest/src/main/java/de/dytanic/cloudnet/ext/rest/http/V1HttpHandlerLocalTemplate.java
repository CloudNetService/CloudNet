package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.http.V1HttpHandler;

public final class V1HttpHandlerLocalTemplate extends V1HttpHandler {

    public V1HttpHandlerLocalTemplate(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "OPTIONS, GET, DELETE, POST");
    }

    @Override
    public void handleGet(String path, IHttpContext context) {
        if (context.request().pathParameters().containsKey("prefix") && context.request().pathParameters().containsKey("name")) {
            ServiceTemplate serviceTemplate = ServiceTemplate.local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));

            if (serviceTemplate.storage().exists()) {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_OK)
                        .header("Content-Type", "application/octet-stream")
                        .header("Content-Disposition", "attachment; filename=\"" + serviceTemplate.getPrefix() + "." + serviceTemplate.getName() + ".zip\"")
                        .body(serviceTemplate.storage().toZipByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext()
                ;
            } else {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_NOT_FOUND)
                        .context()
                        .closeAfter(true)
                        .cancelNext()
                ;
            }

            return;
        }

        context
                .response()
                .statusCode(HttpResponseCode.HTTP_OK)
                .header("Content-Type", "application/json")
                .body(GSON.toJson(CloudNetDriver.getInstance().getLocalTemplateStorage().getTemplates()))
                .context()
                .closeAfter(true)
                .cancelNext()
        ;
    }

    @Override
    public void handlePost(String path, IHttpContext context) {
        if (context.request().pathParameters().containsKey("prefix") && context.request().pathParameters().containsKey("name")) {
            ServiceTemplate serviceTemplate = ServiceTemplate.local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));
            serviceTemplate.storage().deploy(context.request().body());
        }
    }

    @Override
    public void handleDelete(String path, IHttpContext context) {
        if (context.request().pathParameters().containsKey("prefix") && context.request().pathParameters().containsKey("name")) {
            ServiceTemplate serviceTemplate = ServiceTemplate.local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));
            serviceTemplate.storage().delete();

            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;

            return;
        }

        this.send400Response(context, "prefix or name path parameter not found");
    }

}