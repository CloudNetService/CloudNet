/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.rest.http;

import com.google.common.io.ByteStreams;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public final class V1HttpHandlerLocalTemplate extends V1HttpHandler {

  public V1HttpHandlerLocalTemplate(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    this.sendOptions(context, "OPTIONS, GET, DELETE, POST");
  }

  @Override
  public void handleGet(String path, IHttpContext context) {
    if (context.request().pathParameters().containsKey("prefix") && context.request().pathParameters()
      .containsKey("name")) {
      ServiceTemplate serviceTemplate = ServiceTemplate
        .local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));

      if (serviceTemplate.storage().exists()) {
        try (InputStream stream = serviceTemplate.storage().zipTemplate()) {
          if (stream != null) {
            context
              .response()
              .statusCode(HttpResponseCode.HTTP_OK)
              .header("Content-Type", "application/octet-stream")
              .header("Content-Disposition",
                "attachment; filename=\"" + serviceTemplate.getPrefix() + "." + serviceTemplate.getName() + ".zip\"")
              .body(ByteStreams.toByteArray(stream))
              .context()
              .closeAfter(true)
              .cancelNext();
            return;
          }
        } catch (IOException exception) {
          exception.printStackTrace();
        }
      }

      context
        .response()
        .statusCode(HttpResponseCode.HTTP_NOT_FOUND)
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    context
      .response()
      .statusCode(HttpResponseCode.HTTP_OK)
      .header("Content-Type", "application/json")
      .body(GSON.toJson(CloudNetDriver.getInstance().getLocalTemplateStorage().getTemplates()))
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  @Override
  public void handlePost(String path, IHttpContext context) {
    if (context.request().pathParameters().containsKey("prefix") && context.request().pathParameters()
      .containsKey("name")) {
      ServiceTemplate serviceTemplate = ServiceTemplate
        .local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));
      serviceTemplate.storage().deploy(new ZipInputStream(new ByteArrayInputStream(context.request().body())));
    }
  }

  @Override
  public void handleDelete(String path, IHttpContext context) {
    if (context.request().pathParameters().containsKey("prefix") && context.request().pathParameters()
      .containsKey("name")) {
      ServiceTemplate serviceTemplate = ServiceTemplate
        .local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));
      serviceTemplate.storage().delete();

      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .context()
        .closeAfter(true)
        .cancelNext()
      ;

      return;
    }

    this.send400Response(context, "prefix or name path parameter not found");
  }
}
