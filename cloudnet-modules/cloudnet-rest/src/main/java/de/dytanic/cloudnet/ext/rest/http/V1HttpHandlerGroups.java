package de.dytanic.cloudnet.ext.rest.http;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

public final class V1HttpHandlerGroups extends V1HttpHandler {

    private static final Type TYPE = new TypeToken<GroupConfiguration>() {
    }.getType();

    public V1HttpHandlerGroups(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "OPTIONS, GET, POST, DELETE");
    }

    @Override
    public void handleGet(String path, IHttpContext context) {
        if (context.request().pathParameters().containsKey("name")) {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(new JsonDocument("group", GSON.toJson(CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
                            .filter(groupConfiguration -> groupConfiguration.getName().toLowerCase().contains(context.request().pathParameters().get("name")))
                            .findFirst().orElse(null))).toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        } else {
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
                            .filter(groupConfiguration -> !context.request().queryParameters().containsKey("name") ||
                                    this.containsStringElementInCollection(context.request().queryParameters().get("name"), groupConfiguration.getName()))
                            .collect(Collectors.toList())))
                    .context()
                    .closeAfter(true)
                    .cancelNext()
            ;
        }
    }

    @Override
    public void handlePost(String path, IHttpContext context) {
        GroupConfiguration groupConfiguration = GSON.fromJson(new String(context.request().body(), StandardCharsets.UTF_8), TYPE);

        if (groupConfiguration.getName() == null) {
            this.send400Response(context, "groupConfiguration name not found");
            return;
        }

        if (groupConfiguration.getTemplates() == null) {
            groupConfiguration.setTemplates(new ArrayList<>());
        }

        if (groupConfiguration.getIncludes() == null) {
            groupConfiguration.setIncludes(new ArrayList<>());
        }

        if (groupConfiguration.getDeployments() == null) {
            groupConfiguration.setDeployments(new ArrayList<>());
        }

        int status = !CloudNetDriver.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(groupConfiguration.getName()) ?
                HttpResponseCode.HTTP_OK
                :
                HttpResponseCode.HTTP_CREATED;

        CloudNetDriver.getInstance().getGroupConfigurationProvider().addGroupConfiguration(groupConfiguration);
        context.response().statusCode(status);
    }

    @Override
    public void handleDelete(String path, IHttpContext context) {
        if (!context.request().pathParameters().containsKey("name")) {
            this.send400Response(context, "name parameter not found");
            return;
        }

        String name = context.request().pathParameters().get("name");

        if (CloudNetDriver.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name)) {
            CloudNetDriver.getInstance().getGroupConfigurationProvider().removeGroupConfiguration(name);
        }

        context.response().statusCode(HttpResponseCode.HTTP_OK);
    }
}