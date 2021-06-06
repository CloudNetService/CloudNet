package de.dytanic.cloudnet.ext.cloudflare.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.cloudflare.CloudNetCloudflareModule;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfiguration;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class V1CloudflareConfigurationHttpHandler extends V1HttpHandler {

  public V1CloudflareConfigurationHttpHandler(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    this.sendOptions(context, "GET, POST");
  }

  @Override
  public void handleGet(String path, IHttpContext context) {
    context
      .response()
      .statusCode(HttpResponseCode.HTTP_OK)
      .header("Content-Type", "application/json")
      .body(GSON.toJson(CloudNetCloudflareModule.getInstance().getCloudflareConfiguration()))
      .context()
      .closeAfter(true)
      .cancelNext()
    ;
  }

  @Override
  public void handlePost(String path, IHttpContext context) throws Exception {
    try {
      if (context.request().body().length > 0) {
        CloudflareConfiguration cloudflareConfiguration = GSON
          .fromJson(context.request().bodyAsString(), CloudflareConfiguration.TYPE);

        if (cloudflareConfiguration != null) {
          CloudNetCloudflareModule.getInstance().updateConfiguration(cloudflareConfiguration);

          context
            .response()
            .statusCode(HttpResponseCode.HTTP_OK)
            .header("Content-Type", "application")
            .body(new JsonDocument("success", true).toByteArray())
            .context()
            .closeAfter(true)
            .cancelNext()
          ;
        }
      }

    } catch (Exception ex) {

      try (StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer)) {
        ex.printStackTrace(printWriter);
        this.send400Response(context, writer.getBuffer().toString());
      }
    }
  }
}
