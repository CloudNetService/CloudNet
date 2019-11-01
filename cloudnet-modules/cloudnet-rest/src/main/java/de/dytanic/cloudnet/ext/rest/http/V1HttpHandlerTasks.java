package de.dytanic.cloudnet.ext.rest.http;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

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
                    .body(new JsonDocument("task", Iterables.first(CloudNetDriver.getInstance().getServiceTaskProvider().getPermanentServiceTasks(), serviceTask -> serviceTask.getName().toLowerCase().contains(context.request().pathParameters().get("name")))).toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        } else {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(Iterables.filter(CloudNetDriver.getInstance().getServiceTaskProvider().getPermanentServiceTasks(), serviceTask -> !context.request().queryParameters().containsKey("name") ||
                            containsStringElementInCollection(context.request().queryParameters().get("name"), serviceTask.getName()))))
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
            serviceTask.setGroups(Iterables.newArrayList());
        }

        if (serviceTask.getAssociatedNodes() == null) {
            serviceTask.setAssociatedNodes(Iterables.newArrayList());
        }

        if (serviceTask.getTemplates() == null) {
            serviceTask.setTemplates(Iterables.newArrayList());
        }

        if (serviceTask.getIncludes() == null) {
            serviceTask.setIncludes(Iterables.newArrayList());
        }

        if (serviceTask.getDeployments() == null) {
            serviceTask.setDeployments(Iterables.newArrayList());
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