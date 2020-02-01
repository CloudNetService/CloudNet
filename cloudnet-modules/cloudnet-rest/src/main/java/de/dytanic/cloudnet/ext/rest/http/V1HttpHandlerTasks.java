package de.dytanic.cloudnet.ext.rest.http;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

public final class V1HttpHandlerTasks extends V1HttpHandler {

    private static final Type TYPE = new TypeToken<ServiceTask>() {
    }.getType();

    public V1HttpHandlerTasks(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "OPTIONS, GET, DELETE, POST");
    }

    @Override
    public void handleGet(String path, IHttpContext context) {
        if (context.request().pathParameters().containsKey("name")) {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(new JsonDocument("task", CloudNetDriver.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
                            .filter(serviceTask -> serviceTask.getName().toLowerCase().contains(context.request().pathParameters().get("name")))
                            .findFirst().orElse(null)).toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        } else {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(CloudNetDriver.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
                            .filter(serviceTask -> !context.request().queryParameters().containsKey("name") ||
                                    containsStringElementInCollection(context.request().queryParameters().get("name"), serviceTask.getName()))
                            .collect(Collectors.toList())))
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        }
    }

    @Override
    public void handlePost(String path, IHttpContext context) {
        ServiceTask serviceTask = GSON.fromJson(new String(context.request().body(), StandardCharsets.UTF_8), TYPE);

        if (serviceTask.getProcessConfiguration() == null || serviceTask.getName() == null) {
            send400Response(context, "processConfiguration or serviceTask name not found");
            return;
        }

        if (serviceTask.getGroups() == null) {
            serviceTask.setGroups(new ArrayList<>());
        }

        if (serviceTask.getAssociatedNodes() == null) {
            serviceTask.setAssociatedNodes(new ArrayList<>());
        }

        if (serviceTask.getTemplates() == null) {
            serviceTask.setTemplates(new ArrayList<>());
        }

        if (serviceTask.getIncludes() == null) {
            serviceTask.setIncludes(new ArrayList<>());
        }

        if (serviceTask.getDeployments() == null) {
            serviceTask.setDeployments(new ArrayList<>());
        }

        int status = !CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent(serviceTask.getName()) ?
                HttpResponseCode.HTTP_OK
                :
                HttpResponseCode.HTTP_CREATED;

        CloudNetDriver.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
        context
                .response()
                .statusCode(status)
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    @Override
    public void handleDelete(String path, IHttpContext context) {
        if (!context.request().pathParameters().containsKey("name")) {
            send400Response(context, "name parameter not found");
            return;
        }

        String name = context.request().pathParameters().get("name");

        if (CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent(name)) {
            CloudNetDriver.getInstance().getServiceTaskProvider().removePermanentServiceTask(name);
        }

        context
                .response()
                .statusCode(HttpResponseCode.HTTP_OK)
                .context()
                .closeAfter(true)
                .cancelNext();
    }
}