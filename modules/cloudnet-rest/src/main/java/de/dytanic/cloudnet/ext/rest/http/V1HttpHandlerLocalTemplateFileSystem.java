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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class V1HttpHandlerLocalTemplateFileSystem extends V1HttpHandler {

  public V1HttpHandlerLocalTemplateFileSystem(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    this.sendOptions(context, "GET, DELETE, POST");
  }

  private boolean validateTemplate(IHttpContext context) {
    if (!context.request().pathParameters().containsKey("prefix") || !context.request().pathParameters()
      .containsKey("name")) {
      this.send400Response(context, "path parameter prefix or suffix doesn't exists");
      return false;
    }
    return true;
  }

  @Override
  public void handleGet(String path, IHttpContext context) throws IOException {
    if (!this.validateTemplate(context)) {
      return;
    }

    ServiceTemplate serviceTemplate = ServiceTemplate
      .local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));
    if (serviceTemplate.storage().exists()) {
      FileInfo info = this.getFileByPath(path, serviceTemplate);

      if (info == null) {
        this.send404Response(context,
          "file '" + this.parsePath(path) + "' in template '" + serviceTemplate.getTemplatePath() + "' not found");
        return;
      }

      if (info.isDirectory()) {
        FileInfo[] files = serviceTemplate.storage().listFiles(this.parsePath(path));

        context
          .response()
          .statusCode(HttpResponseCode.HTTP_OK)
          .header("Content-Type", "application/json")
          .body(GSON.toJson(files))
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        context
          .response()
          .statusCode(HttpResponseCode.HTTP_OK)
          .header("Content-Type", "application/octet-stream")
          .header("Content-Disposition", "attachment; filename=\"" + info.getName() + "\"")
          .body(FileUtils.toByteArray(serviceTemplate.storage().newInputStream(this.parsePath(path))))
          .context()
          .closeAfter(true)
          .cancelNext();
      }

    } else {
      this.send404Response(context, "template not found!");
    }
  }

  @Override
  public void handlePost(String path, IHttpContext context) throws IOException {
    if (!this.validateTemplate(context)) {
      return;
    }

    ServiceTemplate serviceTemplate = ServiceTemplate
      .local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));
    if (serviceTemplate.storage().exists()) {
      try (OutputStream outputStream = serviceTemplate.storage().newOutputStream(this.parsePath(path))) {
        FileUtils.copy(new ByteArrayInputStream(context.request().body()), outputStream);
      }

      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", "application/json")
        .body(new JsonDocument("success", true).toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();

    } else {
      this.send404Response(context, "template not found!");
    }
  }

  @Override
  public void handleDelete(String path, IHttpContext context) throws IOException {
    if (!this.validateTemplate(context)) {
      return;
    }

    ServiceTemplate serviceTemplate = ServiceTemplate
      .local(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));
    if (serviceTemplate.storage().exists()) {
      String filePath = this.parsePath(path);

      if (!serviceTemplate.storage().hasFile(filePath)) {
        this.send404Response(context,
          "file or directory '" + filePath + "' in template '" + serviceTemplate.getTemplatePath() + "' not found");
        return;
      }

      serviceTemplate.storage().deleteFile(filePath);

      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", "application/json")
        .body(new JsonDocument("success", true).toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();

    } else {
      this.send404Response(context, "template not found!");
    }
  }

  private FileInfo getFileByPath(String path, ServiceTemplate serviceTemplate) throws IOException {
    return serviceTemplate.storage().getFileInfo(this.parsePath(path));
  }

  private String parsePath(String path) {
    String[] relativePathArray = path.split("/files");
    return relativePathArray.length == 1 ? "" : relativePathArray[1].substring(1);
  }

  private void send404Response(IHttpContext context, String reason) {
    Preconditions.checkNotNull(context);
    Preconditions.checkNotNull(reason);

    context
      .response()
      .statusCode(HttpResponseCode.HTTP_NOT_FOUND)
      .header("Content-Type", "application/json")
      .body(new JsonDocument("reason", reason).toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext()
    ;
  }
}
