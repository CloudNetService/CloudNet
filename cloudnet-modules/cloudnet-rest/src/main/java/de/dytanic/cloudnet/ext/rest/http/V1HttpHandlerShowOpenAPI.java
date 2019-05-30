package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.MethodHttpHandlerAdapter;
import java.io.InputStream;

public final class V1HttpHandlerShowOpenAPI extends MethodHttpHandlerAdapter {

  @Override
  public void handleGet(String path, IHttpContext context) throws Exception {
    try (InputStream inputStream = V1HttpHandlerShowOpenAPI.class
      .getClassLoader().getResourceAsStream("openapi/v1-openapi.yml")) {
      if (inputStream != null) {
        context
          .response()
          .statusCode(HttpResponseCode.HTTP_OK)
          .header("Content-Type", "text/plain")
          .body(FileUtils.toByteArray(inputStream))
          .context()
          .closeAfter(true)
          .cancelNext()
        ;
      }
    }
  }
}