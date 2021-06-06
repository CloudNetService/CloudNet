package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

public class V1HttpHandlerLogout extends V1HttpHandler {

  public V1HttpHandlerLogout() {
    super(null);
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    this.sendOptions(context, "OPTIONS");
  }

  @Override
  public void handle(String path, IHttpContext context) throws Exception {
    super.handle(path, context);

    if (context.request().method().equalsIgnoreCase("OPTIONS")) {
      return;
    }

    HTTP_SESSION.logout(context);

    context
      .response()
      .statusCode(HttpResponseCode.HTTP_OK)
      .context()
      .closeAfter(true)
      .cancelNext();
  }
}
