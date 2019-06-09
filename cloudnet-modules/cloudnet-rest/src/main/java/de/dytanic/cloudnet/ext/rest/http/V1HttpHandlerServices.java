package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.util.Queue;

public final class V1HttpHandlerServices extends V1HttpHandler {

    public V1HttpHandlerServices(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) throws Exception {
        this.sendOptions(context, "OPTIONS, GET");
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        if (context.request().pathParameters().containsKey("uuid")) {
            ServiceInfoSnapshot serviceInfoSnapshot = Iterables.first(CloudNetDriver.getInstance().getCloudServices(), serviceInfoSnapshot12 -> serviceInfoSnapshot12.getServiceId().getUniqueId().toString().contains(context.request().pathParameters().get("uuid")));

            if (serviceInfoSnapshot == null) {
                serviceInfoSnapshot = Iterables.first(CloudNetDriver.getInstance().getCloudServices(), serviceInfoSnapshot1 -> serviceInfoSnapshot1.getServiceId().getName().contains(context.request().pathParameters().get("uuid")));
            }

            if (serviceInfoSnapshot == null) {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_NOT_FOUND)
                        .context()
                        .closeAfter(true)
                        .cancelNext()
                ;

                return;
            }

            if (context.request().pathParameters().containsKey("operation")) {
                switch (context.request().pathParameters().get("operation").toLowerCase()) {
                    case "start": {
                        CloudNetDriver.getInstance().setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.RUNNING);
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(serviceInfoSnapshot))
                        ;
                    }
                    break;
                    case "stop": {
                        CloudNetDriver.getInstance().setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.STOPPED);
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(serviceInfoSnapshot))
                        ;
                    }
                    break;
                    case "delete": {
                        CloudNetDriver.getInstance().setCloudServiceLifeCycle(serviceInfoSnapshot, ServiceLifeCycle.DELETED);
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(serviceInfoSnapshot))
                        ;
                    }
                    break;
                    case "log": {
                        Queue<String> queue = CloudNetDriver.getInstance().getCachedLogMessagesFromService(serviceInfoSnapshot.getServiceId().getUniqueId());

                        StringBuilder stringBuilder = new StringBuilder();

                        for (String item : queue) stringBuilder.append(item).append("\n");

                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "text/plain")
                                .body(stringBuilder.toString())
                        ;
                    }
                    break;
                    case "log_json": {
                        Queue<String> queue = CloudNetDriver.getInstance().getCachedLogMessagesFromService(serviceInfoSnapshot.getServiceId().getUniqueId());

                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "application/json")
                                .body(GSON.toJson(queue))
                        ;
                    }
                    break;
                }
            } else {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_OK)
                        .header("Content-Type", "application/json")
                        .body(GSON.toJson(serviceInfoSnapshot))
                ;
            }

            context
                    .closeAfter(true)
                    .cancelNext()
            ;
            return;
        }

        context
                .response()
                .header("Content-Type", "application/json")
                .body(GSON.toJson(Iterables.filter(CloudNetDriver.getInstance().getCloudServices(), serviceInfoSnapshot -> {
                    if (context.request().queryParameters().containsKey("name") &&
                            !context.request().queryParameters().get("name").contains(serviceInfoSnapshot.getServiceId().getName()))
                        return false;

                    if (context.request().queryParameters().containsKey("task") &&
                            !context.request().queryParameters().get("task").contains(serviceInfoSnapshot.getServiceId().getTaskName()))
                        return false;

                    return !context.request().queryParameters().containsKey("node") ||
                            context.request().queryParameters().get("node").contains(serviceInfoSnapshot.getServiceId().getNodeUniqueId());
                })))
                .statusCode(200)
                .context()
                .closeAfter(true)
                .cancelNext()
        ;
    }
}