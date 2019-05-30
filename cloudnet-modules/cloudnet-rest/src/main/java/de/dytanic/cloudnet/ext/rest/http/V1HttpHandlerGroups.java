package de.dytanic.cloudnet.ext.rest.http;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

public final class V1HttpHandlerGroups extends V1HttpHandler {

  private static final Type TYPE = new TypeToken<GroupConfiguration>() {
  }.getType();

  public V1HttpHandlerGroups(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context)
    throws Exception {
    this.sendOptions(context, "OPTIONS, GET, POST, DELETE");
  }

  @Override
  public void handleGet(String path, IHttpContext context) throws Exception {
    if (context.request().pathParameters().containsKey("name")) {
      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", "application/json")
        .body(new JsonDocument("group", GSON.toJson(Iterables
          .first(CloudNetDriver.getInstance().getGroupConfigurations(),
            new Predicate<GroupConfiguration>() {
              @Override
              public boolean test(GroupConfiguration groupConfiguration) {
                return groupConfiguration.getName().toLowerCase()
                  .contains(
                    context.request().pathParameters().get("name"));
              }
            }))).toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext()
      ;
    } else {
      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", "application/json")
        .body(GSON.toJson(Iterables
          .filter(CloudNetDriver.getInstance().getGroupConfigurations(),
            new Predicate<GroupConfiguration>() {
              @Override
              public boolean test(GroupConfiguration groupConfiguration) {
                if (context.request().queryParameters()
                  .containsKey("name") &&
                  !containsStringElementInCollection(
                    context.request().queryParameters().get("name"),
                    groupConfiguration.getName())) {
                  return false;
                }

                return true;
              }
            })))
        .context()
        .closeAfter(true)
        .cancelNext()
      ;
    }
  }

  @Override
  public void handlePost(String path, IHttpContext context) throws Exception {
    GroupConfiguration groupConfiguration = GSON
      .fromJson(new String(context.request().body(), StandardCharsets.UTF_8),
        TYPE);

    if (groupConfiguration.getName() == null) {
      send400Response(context, "groupConfiguration name not found");
      return;
    }

    if (groupConfiguration.getTemplates() == null) {
      groupConfiguration.setTemplates(Iterables.newArrayList());
    }

    if (groupConfiguration.getIncludes() == null) {
      groupConfiguration.setIncludes(Iterables.newArrayList());
    }

    if (groupConfiguration.getDeployments() == null) {
      groupConfiguration.setDeployments(Iterables.newArrayList());
    }

    int status = !CloudNetDriver.getInstance()
      .isGroupConfigurationPresent(groupConfiguration.getName()) ?
      HttpResponseCode.HTTP_OK
      :
        HttpResponseCode.HTTP_CREATED;

    CloudNetDriver.getInstance().addGroupConfiguration(groupConfiguration);
    context.response().statusCode(status);
  }

  @Override
  public void handleDelete(String path, IHttpContext context) throws Exception {
    if (!context.request().pathParameters().containsKey("name")) {
      send400Response(context, "name parameter not found");
      return;
    }

    String name = context.request().pathParameters().get("name");

    if (CloudNetDriver.getInstance().isGroupConfigurationPresent(name)) {
      CloudNetDriver.getInstance().removeGroupConfiguration(name);
    }

    context.response().statusCode(HttpResponseCode.HTTP_OK);
  }
}