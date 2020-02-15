package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.util.stream.Collectors;

public final class V1HttpHandlerModules extends V1HttpHandler {

    public V1HttpHandlerModules(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "OPTIONS, GET");
    }

    @Override
    public void handleGet(String path, IHttpContext context) {
        if (context.request().pathParameters().containsKey("name")) {
            context
                    .response()
                    .header("Content-Type", "application/json")
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .body(GSON.toJson(CloudNetDriver.getInstance().getModuleProvider().getModules().stream()
                            .filter(moduleWrapper -> context.request().pathParameters().get("name").contains(moduleWrapper.getModuleConfiguration().getName()))
                            .collect(Collectors.toList())))
            ;
        } else {
            context
                    .response()
                    .header("Content-Type", "application/json")
                    .statusCode(200)
                    .body(GSON.toJson(CloudNetDriver.getInstance().getModuleProvider().getModules().stream()
                            .map(IModuleWrapper::getModuleConfiguration)
                            .filter(moduleConfiguration -> {
                                if (context.request().queryParameters().containsKey("group") &&
                                        !containsStringElementInCollection(context.request().queryParameters().get("group"), moduleConfiguration.getGroup())) {
                                    return false;
                                }

                                if (context.request().queryParameters().containsKey("name") &&
                                        !containsStringElementInCollection(context.request().queryParameters().get("name"), moduleConfiguration.getName())) {
                                    return false;
                                }

                                return !context.request().queryParameters().containsKey("version") ||
                                        containsStringElementInCollection(context.request().queryParameters().get("version"), moduleConfiguration.getVersion());
                            }).collect(Collectors.toList())))
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        }
    }
}