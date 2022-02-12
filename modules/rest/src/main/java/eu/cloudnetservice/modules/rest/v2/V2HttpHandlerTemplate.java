/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponse;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.driver.template.SpecificTemplateStorage;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import eu.cloudnetservice.cloudnet.node.template.install.InstallInformation;
import eu.cloudnetservice.cloudnet.node.template.install.ServiceVersion;
import eu.cloudnetservice.cloudnet.node.template.install.ServiceVersionType;
import eu.cloudnetservice.modules.rest.RestUtil;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class V2HttpHandlerTemplate extends V2HttpHandler {

  public V2HttpHandlerTemplate(@Nullable String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(@NonNull String path, @NonNull HttpContext context, @NonNull HttpSession ses) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.contains("/file/")) {
        if (path.contains("/download")) {
          this.handleFileDownloadRequest(context);
        } else if (path.contains("/info")) {
          this.handleFileInfoRequest(context);
        } else if (path.contains("/exists")) {
          this.handleFileExistsRequest(context);
        }
      } else if (path.endsWith("/download")) {
        this.handleDownloadRequest(context);
      } else if (path.contains("/directory/")) {
        if (path.contains("/list")) {
          this.handleFileListRequest(context);
        }
      } else if (path.endsWith("/create")) {
        this.handleCreateRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      if (path.endsWith("/deploy")) {
        this.handleDeployRequest(context);
      } else if (path.contains("/file/")) {
        if (path.contains("/create")) {
          this.handleFileWriteRequest(context, false);
        } else if (path.contains("/append")) {
          this.handleFileWriteRequest(context, true);
        }
      } else if (path.contains("/directory/")) {
        if (path.contains("/create")) {
          this.handleDirectoryCreateRequest(context);
        }
      } else if (path.endsWith("/install")) {
        this.handleInstallationRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      if (path.contains("/file") || path.contains("/directory")) {
        this.handleFileDeleteRequest(context);
      } else {
        this.handleTemplateDeleteRequest(context);
      }
    }
  }

  protected void handleDownloadRequest(@NonNull HttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      var stream = storage.zipTemplateAsync().get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unable to zip template").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context, "application/zip; charset=UTF-8")
          .body(stream)
          .header("Content-Disposition", "attachment; filename="
            + template.toString().replace('/', '_') + ".zip")
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleFileDownloadRequest(@NonNull HttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      var stream = storage.newInputStreamAsync(path).get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Missing file or path is directory").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        var fileName = this.guessFileName(path);
        this.ok(context, "application/octet-stream")
          .header("Content-Disposition",
            String.format("attachment%s", fileName == null ? "" : "; filename=" + fileName))
          .body(stream)
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleFileInfoRequest(@NonNull HttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      var info = storage.fileInfoAsync(path).get();
      if (info == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unknown file or directory").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.success().append("info", info).toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleFileExistsRequest(@NonNull HttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.hasFileAsync(path).get();
      this.ok(context)
        .body(this.success().append("exists", status).toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleFileListRequest(@NonNull HttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      var dir = RestUtil.first(context.request().queryParameters().get("directory"), "");
      var deep = Boolean.parseBoolean(RestUtil.first(context.request().queryParameters().get("deep"), "false"));

      var files = storage.listFilesAsync(dir, deep).get();
      this.ok(context)
        .body(this.success().append("files", files).toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleCreateRequest(@NonNull HttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.createAsync().get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleDeployRequest(@NonNull HttpContext context) {
    var stream = context.request().bodyStream();
    if (stream == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing data in body").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.deployAsync(stream).get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleFileDeleteRequest(@NonNull HttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.deleteFileAsync(path).get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleTemplateDeleteRequest(@NonNull HttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.deleteAsync().get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleInstallationRequest(@NonNull HttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      var body = this.body(context.request());

      var versionType = body.get("type", ServiceVersionType.class);
      if (versionType == null) {
        versionType = this.node().serviceVersionProvider()
          .getServiceVersionType(body.getString("typeName", "")).orElse(null);
        if (versionType == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "No service type or type name provided").toString())
            .context()
            .closeAfter(true)
            .cancelNext();
          return;
        }
      }

      var version = body.get("version", ServiceVersion.class);
      if (version == null) {
        version = versionType.version(body.getString("versionName", "")).orElse(null);
        if (version == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "Missing version or version name").toString())
            .context()
            .closeAfter(true)
            .cancelNext();
          return;
        }
      }

      var forceInstall = body.getBoolean("force", false);
      var cacheFiles = body.getBoolean("caches", version.cacheFiles());

      var installInformation = InstallInformation.builder()
        .serviceVersion(version)
        .serviceVersionType(versionType)
        .cacheFiles(cacheFiles)
        .toTemplate(template)
        .build();

      if (this.node().serviceVersionProvider()
        .installServiceVersion(installInformation, forceInstall)) {
        this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
      } else {
        this.ok(context).body(this.failure().toString()).context().closeAfter(true).cancelNext();
      }
    });
  }

  protected void handleFileWriteRequest(@NonNull HttpContext context, boolean append) {
    var content = context.request().bodyStream();
    if (content == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing input from body").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      var task = append ? storage.appendOutputStreamAsync(path) : storage.newOutputStreamAsync(path);
      var stream = task.get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unable to open file stream").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        try {
          content.transferTo(stream);
          this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
        } catch (IOException exception) {
          this.notifyException(context, exception);
        }
      }
    });
  }

  protected void handleDirectoryCreateRequest(@NonNull HttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.createDirectoryAsync(path).get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleWithTemplateContext(
    @NonNull HttpContext context,
    @NonNull ThrowableBiConsumer<ServiceTemplate, SpecificTemplateStorage, Exception> handler
  ) {
    var storage = context.request().pathParameters().get("storage");
    var prefix = context.request().pathParameters().get("prefix");
    var name = context.request().pathParameters().get("name");

    if (storage == null || prefix == null || name == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing storage, prefix or name in path parameters").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var template = ServiceTemplate.builder().prefix(prefix).name(name).storage(storage).build();
    var templateStorage = template.knownStorage();

    if (templateStorage == null) {
      this.ok(context)
        .body(this.failure().append("reason", "Unknown template storage").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    try {
      handler.accept(template, templateStorage);
    } catch (Exception exception) {
      this.notifyException(context, exception);
    }
  }

  protected void handleWithFileTemplateContext(
    @NonNull HttpContext context,
    @NonNull ThrowableTriConsumer<ServiceTemplate, SpecificTemplateStorage, String, Exception> handler
  ) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      var fileName = RestUtil.first(context.request().queryParameters().get("path"), null);
      if (fileName == null) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing file name in path").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
        return;
      }

      handler.accept(template, storage, fileName);
    });
  }

  protected void notifyException(@NonNull HttpContext context, @NonNull Exception exception) {
    LOGGER.fine("Exception handling template request", exception);
    this.response(context, HttpResponseCode.INTERNAL_SERVER_ERROR)
      .body(this.failure().append("reason", "Exception processing request").toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected @NonNull HttpResponse ok(@NonNull HttpContext context, @NonNull String contentType) {
    return context.response()
      .status(HttpResponseCode.OK)
      .header("Content-Type", contentType)
      .header("Access-Control-Allow-Origin", this.accessControlConfiguration.corsPolicy());
  }

  protected @Nullable String guessFileName(@NonNull String path) {
    var index = path.lastIndexOf('/');
    if (index == -1 || index + 1 == path.length()) {
      return null;
    } else {
      return path.substring(index);
    }
  }

  @FunctionalInterface
  protected interface ThrowableBiConsumer<T, U, E extends Throwable> {

    void accept(T t, U u) throws E;
  }

  @FunctionalInterface
  protected interface ThrowableTriConsumer<T, U, F, E extends Throwable> {

    void accept(T t, U u, F f) throws E;
  }
}
