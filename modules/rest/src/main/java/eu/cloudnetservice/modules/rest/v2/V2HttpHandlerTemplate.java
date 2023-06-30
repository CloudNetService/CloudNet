/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpResponse;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.FirstRequestQueryParam;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.Optional;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import eu.cloudnetservice.node.version.ServiceVersion;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import eu.cloudnetservice.node.version.ServiceVersionType;
import eu.cloudnetservice.node.version.information.TemplateVersionInstaller;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@HandlerPermission("http.v2.template")
@ApplyHeaders
public final class V2HttpHandlerTemplate extends V2HttpHandler {

  private static final Logger LOGGER = LogManager.logger(V2HttpHandlerTemplate.class);

  private final ServiceVersionProvider versionProvider;

  @Inject
  public V2HttpHandlerTemplate(@NonNull ServiceVersionProvider versionProvider) {
    this.versionProvider = versionProvider;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/download")
  private void handleDownloadRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var stream = storage.zipTemplateAsync(template).get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unable to zip template").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      } else {
        this.ok(context, "application/zip; charset=UTF-8")
          .body(stream)
          .header("Content-Disposition", "attachment; filename="
            + template.toString().replace('/', '_') + ".zip")
          .context()
          .closeAfter(true)
          .cancelNext(true);
      }
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/file/download")
  private void handleFileDownloadRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var stream = storage.newInputStream(template, path);
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Missing file or path is directory").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      } else {
        var fileName = this.guessFileName(path);
        this.ok(context, "application/octet-stream")
          .header("Content-Disposition",
            String.format("attachment%s", fileName == null ? "" : "; filename=" + fileName))
          .body(stream)
          .context()
          .closeAfter(true)
          .cancelNext(true);
      }
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/file/info")
  private void handleFileInfoRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var info = storage.fileInfo(template, path);
      if (info == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unknown file or directory").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      } else {
        this.ok(context)
          .body(this.success().append("info", info).toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      }
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/file/exists")
  private void handleFileExistsRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var status = storage.hasFile(template, path);
      this.ok(context)
        .body(this.success().append("exists", status).toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/directory/list")
  private void handleFileListRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @Optional @FirstRequestQueryParam(value = "directory", def = "") String directory,
    @NonNull @Optional @FirstRequestQueryParam(value = "deep", def = "false") String deep
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var files = storage.listFiles(template, directory, Boolean.parseBoolean(deep));
      this.ok(context)
        .body(this.success().append("files", files).toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/create", methods = "PUT")
  private void handleCreateRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var status = storage.create(template);
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/deploy", methods = "POST")
  private void handleDeployRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @RequestBody InputStream body
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var status = storage.deploy(template, body);
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/file", methods = "DELETE")
  private void handleFileDeleteRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var status = storage.deleteFile(template, path);
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}", methods = "DELETE")
  private void handleTemplateDeleteRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var status = storage.delete(template);
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/install", methods = "POST")
  private void handleInstallationRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @RequestBody Document body
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var versionType = body.readObject("type", ServiceVersionType.class);
      if (versionType == null) {
        versionType = this.versionProvider.getServiceVersionType(body.getString("typeName", ""));
        if (versionType == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "No service type or type name provided").toString())
            .context()
            .closeAfter(true)
            .cancelNext(true);
          return;
        }
      }

      var version = body.readObject("version", ServiceVersion.class);
      if (version == null) {
        version = versionType.version(body.getString("versionName", ""));
        if (version == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "Missing version or version name").toString())
            .context()
            .closeAfter(true)
            .cancelNext(true);
          return;
        }
      }

      var forceInstall = body.getBoolean("force", false);
      var cacheFiles = body.getBoolean("caches", version.cacheFiles());

      var installer = TemplateVersionInstaller.builder()
        .serviceVersion(version)
        .serviceVersionType(versionType)
        .cacheFiles(cacheFiles)
        .toTemplate(template)
        .build();

      if (this.versionProvider.installServiceVersion(installer, forceInstall)) {
        this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
      } else {
        this.ok(context).body(this.failure().toString()).context().closeAfter(true).cancelNext(true);
      }
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/directory/create", methods = "POST")
  private void handleDirectoryCreateRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @FirstRequestQueryParam("path") String path
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var status = storage.createDirectory(template, path);
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    });
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/file/create", methods = "POST")
  private void handleFileCreateRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @FirstRequestQueryParam("path") String path,
    @NonNull @RequestBody InputStream body
  ) {
    this.handleFileWriteRequest(context, storageName, prefix, templateName, path, body, false);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/template/{storage}/{prefix}/{name}/file/append", methods = "POST")
  private void handleFileAppendRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @FirstRequestQueryParam("path") String path,
    @NonNull @RequestBody InputStream body
  ) {
    this.handleFileWriteRequest(context, storageName, prefix, templateName, path, body, true);
  }

  private void handleFileWriteRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("storage") String storageName,
    @NonNull @RequestPathParam("prefix") String prefix,
    @NonNull @RequestPathParam("name") String templateName,
    @NonNull @FirstRequestQueryParam("path") String path,
    @NonNull @RequestBody InputStream body,
    boolean append
  ) {
    this.handleWithTemplateContext(context, storageName, prefix, templateName, (template, storage) -> {
      var stream = append
        ? storage.appendOutputStream(template, path)
        : storage.newOutputStream(template, path);
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unable to open file stream").toString())
          .context()
          .closeAfter(true)
          .cancelNext(true);
      } else {
        try {
          body.transferTo(stream);
          this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext(true);
        } catch (IOException exception) {
          this.notifyException(context, exception);
        }
      }
    });
  }

  private void handleWithTemplateContext(
    @NonNull HttpContext context,
    @NonNull String storage,
    @NonNull String prefix,
    @NonNull String name,
    @NonNull ThrowableBiConsumer<ServiceTemplate, TemplateStorage, Exception> handler
  ) {
    var template = ServiceTemplate.builder().prefix(prefix).name(name).storage(storage).build();
    var templateStorage = template.findStorage();

    if (templateStorage == null) {
      this.ok(context)
        .body(this.failure().append("reason", "Unknown template storage").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    try {
      handler.accept(template, templateStorage);
    } catch (Exception exception) {
      this.notifyException(context, exception);
    }
  }

  private void notifyException(@NonNull HttpContext context, @NonNull Exception exception) {
    LOGGER.fine("Exception handling template request", exception);
    this.response(context, HttpResponseCode.INTERNAL_SERVER_ERROR)
      .body(this.failure().append("reason", "Exception processing request").toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  private @NonNull HttpResponse ok(@NonNull HttpContext context, @NonNull String contentType) {
    return context.response()
      .status(HttpResponseCode.OK)
      .header("Content-Type", contentType);
  }

  private @Nullable String guessFileName(@NonNull String path) {
    var index = path.lastIndexOf('/');
    if (index == -1 || index + 1 == path.length()) {
      return null;
    } else {
      return path.substring(index);
    }
  }

  @FunctionalInterface
  private interface ThrowableBiConsumer<T, U, E extends Throwable> {

    void accept(T t, U u) throws E;
  }
}
