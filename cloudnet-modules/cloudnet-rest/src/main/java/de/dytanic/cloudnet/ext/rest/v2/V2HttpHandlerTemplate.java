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

package de.dytanic.cloudnet.ext.rest.v2;

import com.google.common.io.ByteStreams;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpResponse;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.ext.rest.RestUtils;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class V2HttpHandlerTemplate extends V2HttpHandler {

  public V2HttpHandlerTemplate(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) throws Exception {
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

  protected void handleDownloadRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      InputStream stream = storage.zipTemplateAsync().throwExceptionOnFailure().get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unable to zip template").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context, "application/zip; charset=UTF-8")
          .body(stream)
          .header("Content-Disposition", "attachment; filename="
            + template.getTemplatePath().replace('/', '_') + ".zip")
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleFileDownloadRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      InputStream stream = storage.newInputStreamAsync(path).throwExceptionOnFailure().get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Missing file or path is directory").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        String fileName = this.guessFileName(path);
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

  protected void handleFileInfoRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      FileInfo info = storage.getFileInfoAsync(path).throwExceptionOnFailure().get();
      if (info == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unknown file or directory").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.success().append("info", info).toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleFileExistsRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.hasFileAsync(path).throwExceptionOnFailure().get();
      this.ok(context)
        .body(this.success().append("exists", status).toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleFileListRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      String dir = RestUtils.getFirst(context.request().queryParameters().get("directory"), "");
      boolean deep = Boolean.parseBoolean(RestUtils.getFirst(context.request().queryParameters().get("deep"), "false"));

      FileInfo[] files = storage.listFilesAsync(dir, deep).throwExceptionOnFailure().get();
      this.ok(context)
        .body(this.success().append("files", files).toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleCreateRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.createAsync().fireExceptionOnFailure().get();
      this.ok(context)
        .body(status ? this.success().toByteArray() : this.failure().toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleDeployRequest(IHttpContext context) {
    InputStream stream = context.request().bodyStream();
    if (stream == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing data in body").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.deployAsync(stream).throwExceptionOnFailure().get();
      this.ok(context)
        .body(status ? this.success().toByteArray() : this.failure().toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleFileDeleteRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.deleteFileAsync(path).throwExceptionOnFailure().get();
      this.ok(context)
        .body(status ? this.success().toByteArray() : this.failure().toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleTemplateDeleteRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.deleteAsync().throwExceptionOnFailure().get();
      this.ok(context)
        .body(status ? this.success().toByteArray() : this.failure().toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleInstallationRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      JsonDocument body = this.body(context.request());

      ServiceVersionType versionType = body.get("type", ServiceVersionType.class);
      if (versionType == null) {
        versionType = this.getCloudNet().getServiceVersionProvider()
          .getServiceVersionType(body.getString("typeName", "")).orElse(null);
        if (versionType == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "No service type or type name provided").toByteArray())
            .context()
            .closeAfter(true)
            .cancelNext();
          return;
        }
      }

      ServiceVersion version = body.get("version", ServiceVersion.class);
      if (version == null) {
        version = versionType.getVersion(body.getString("versionName", "")).orElse(null);
        if (version == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "Missing version or version name").toByteArray())
            .context()
            .closeAfter(true)
            .cancelNext();
          return;
        }
      }

      boolean forceInstall = body.getBoolean("force", false);
      if (this.getCloudNet().getServiceVersionProvider()
        .installServiceVersion(versionType, version, template, forceInstall)) {
        this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
      } else {
        this.ok(context).body(this.failure().toByteArray()).context().closeAfter(true).cancelNext();
      }
    });
  }

  protected void handleFileWriteRequest(IHttpContext context, boolean append) {
    InputStream content = context.request().bodyStream();
    if (content == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing input from body").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      ITask<OutputStream> task = append ? storage.appendOutputStreamAsync(path) : storage.newOutputStreamAsync(path);
      OutputStream stream = task.throwExceptionOnFailure().get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unable to open file stream").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        try {
          ByteStreams.copy(content, stream);
          this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
        } catch (IOException exception) {
          this.notifyException(context, exception);
        }
      }
    });
  }

  protected void handleDirectoryCreateRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.createDirectoryAsync(path).throwExceptionOnFailure().get();
      this.ok(context)
        .body(status ? this.success().toByteArray() : this.failure().toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleWithTemplateContext(IHttpContext context,
    ThrowableBiConsumer<ServiceTemplate, SpecificTemplateStorage, Exception> handler) {
    String storage = context.request().pathParameters().get("storage");
    String prefix = context.request().pathParameters().get("prefix");
    String name = context.request().pathParameters().get("name");

    if (storage == null || prefix == null || name == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing storage, prefix or name in path parameters").toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    ServiceTemplate template = new ServiceTemplate(prefix, name, storage);
    SpecificTemplateStorage templateStorage = template.nullableStorage();

    if (templateStorage == null) {
      this.ok(context)
        .body(this.failure().append("reason", "Unknown template storage").toByteArray())
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

  protected void handleWithFileTemplateContext(IHttpContext context,
    ThrowableTriConsumer<ServiceTemplate, SpecificTemplateStorage, String, Exception> handler) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      String fileName = RestUtils.getFirst(context.request().queryParameters().get("path"), null);
      if (fileName == null) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing file name in path").toByteArray())
          .context()
          .closeAfter(true)
          .cancelNext();
        return;
      }

      handler.accept(template, storage, fileName);
    });
  }

  protected void notifyException(IHttpContext context, Exception exception) {
    this.getCloudNet().getLogger().debug("Exception handling template request", exception);
    this.response(context, HttpResponseCode.HTTP_INTERNAL_ERROR)
      .body(this.failure().append("reason", "Exception processing request").toByteArray())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected IHttpResponse ok(@NotNull IHttpContext context, @NotNull String contentType) {
    return context.response()
      .statusCode(HttpResponseCode.HTTP_OK)
      .header("Content-Type", contentType)
      .header("Access-Control-Allow-Origin", this.accessControlConfiguration.getCorsPolicy());
  }

  protected @Nullable String guessFileName(String path) {
    int index = path.lastIndexOf('/');
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
