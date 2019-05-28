package de.dytanic.cloudnet.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.gson.JsonDocumentTypeAdapter;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.MethodHttpHandlerAdapter;
import de.dytanic.cloudnet.driver.permission.Permission;
import java.util.Collection;
import lombok.Getter;

public abstract class V1HttpHandler extends MethodHttpHandlerAdapter {

  protected static final Gson GSON = new GsonBuilder()
      .registerTypeAdapterFactory(TypeAdapters.newTypeHierarchyFactory(
          JsonDocument.class,
          new JsonDocumentTypeAdapter()
      )).serializeNulls().disableHtmlEscaping().create();

  protected static final V1HttpSession HTTP_SESSION = new V1HttpSession();

  @Getter
  private final String permission;

  public V1HttpHandler(String permission) {
    this.permission = permission;
  }

  @Override
  public void handle(String path, IHttpContext context) throws Exception {
    context.response().header("Access-Control-Allow-Origin", "*");

    if (this.permission != null && !this.checkPermission(context,
        this.permission + "." + context.request().method().toLowerCase())) {
      this.send403Response(context,
          "permission required " + this.permission + "." + context.request()
              .method().toLowerCase());
      return;
    }

    super.handle(path, context);
  }

  protected IHttpContext send400Response(IHttpContext context, String reason) {
    context
        .response()
        .statusCode(HttpResponseCode.HTTP_BAD_REQUEST)
        .header("Content-Type", "application/json")
        .body(new JsonDocument("success", false).append("reason", reason)
            .toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext()
    ;

    return context;
  }

  protected IHttpContext send403Response(IHttpContext context, String reason) {
    context
        .response()
        .statusCode(HttpResponseCode.HTTP_FORBIDDEN)
        .header("Content-Type", "application/json")
        .body(new JsonDocument("success", true).append("reason", reason)
            .toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext()
    ;

    return context;
  }

  protected IHttpContext sendOptions(IHttpContext context,
      String allowedMethods) {
    context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Allow", allowedMethods)
        .header("Content-Type", "application/json")
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true")
        .header("Access-Control-Allow-Headers",
            "Accept, Origin, if-none-match, Access-Control-Allow-Headers, Access-Control-Allow-Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")
        .header("Access-Control-Expose-Headers",
            "Accept, Origin, if-none-match, Access-Control-Allow-Headers, Access-Control-Allow-Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")
        .header("Access-Control-Allow-Methods", allowedMethods)
        .header("Access-Control-Max-Age", "3600")
        .context()
        .closeAfter(true)
    ;

    return context;
  }

  protected boolean checkPermission(IHttpContext context, String permission) {
    try {
      if (!permission.isEmpty() && !getCloudNet().getPermissionManagement()
          .hasPermission(HTTP_SESSION.getUser(context),
              new Permission(permission, 1))) {
        this.send403Response(context, "permission required " + permission);
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return true;
  }

  protected boolean containsStringElementInCollection(
      Collection<String> collection, String name) {
    Validate.checkNotNull(collection);
    Validate.checkNotNull(name);

    for (String queryString : collection) {
      if (queryString.contains(name)) {
        return true;
      }
    }

    return false;
  }

  protected final CloudNet getCloudNet() {
    return CloudNet.getInstance();
  }
}