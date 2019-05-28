package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.http.V1HttpHandler;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorageUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Collection;

public final class V1HttpHandlerLocalTemplateFileSystem extends V1HttpHandler {

  public V1HttpHandlerLocalTemplateFileSystem(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context)
    throws Exception {
    this.sendOptions(context, "GET, DELETE, POST");
  }

  @Override
  public void handleGet(String path, IHttpContext context) throws Exception {
    if (!context.request().pathParameters().containsKey("prefix") || !context
      .request().pathParameters().containsKey("name")) {
      this.send400Response(context,
        "path parameter prefix or suffix doesn't exists");
      return;
    }

    ServiceTemplate serviceTemplate = createLocalTemplate(
      context.request().pathParameters().get("prefix"),
      context.request().pathParameters().get("name"));

    if (LocalTemplateStorageUtil.getLocalTemplateStorage()
      .has(serviceTemplate)) {
      File file = getFileByPath(path, serviceTemplate);

      if (file == null || !file.exists()) {
        this.send404Response(context,
          "file '" + file.getName() + "' in template '" + serviceTemplate
            .getTemplatePath() + "' not found");
        return;
      }

      if (file.isDirectory()) {
        File[] files = file.listFiles();

        if (files != null) {
          Collection<JsonDocument> documents = Iterables
            .newArrayList(files.length);

          for (File item : files) {
            documents.add(getFileEntry(item));
          }

          context
            .response()
            .statusCode(HttpResponseCode.HTTP_OK)
            .header("Content-Type", "application/json")
            .body(GSON.toJson(documents))
            .context()
            .closeAfter(true)
            .cancelNext()
          ;

        } else {
          this.send404Response(context,
            "directory is empty or not a directory");
        }
      } else {
        context
          .response()
          .statusCode(HttpResponseCode.HTTP_OK)
          .header("Content-Type", "application/octet-stream")
          .header("Content-Disposition",
            "attachment; filename=\"" + file.getName() + "\"")
          .body(Files.readAllBytes(file.toPath()))
          .context()
          .closeAfter(true)
          .cancelNext()
        ;
      }

    } else {
      this.send404Response(context, "template not found!");
    }
  }

  @Override
  public void handlePost(String path, IHttpContext context) throws Exception {
    if (!context.request().pathParameters().containsKey("prefix") || !context
      .request().pathParameters().containsKey("name")) {
      this.send400Response(context,
        "path parameter prefix or suffix doesn't exists");
      return;
    }

    ServiceTemplate serviceTemplate = createLocalTemplate(
      context.request().pathParameters().get("prefix"),
      context.request().pathParameters().get("name"));

    if (LocalTemplateStorageUtil.getLocalTemplateStorage()
      .has(serviceTemplate)) {
      File file = getFileByPath(path, serviceTemplate);

      if (!file.exists()) {
        file.getParentFile().mkdirs();
        file.createNewFile();

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
          context.request().body());
          FileOutputStream fileOutputStream = new FileOutputStream(file)) {
          FileUtils.copy(byteArrayInputStream, fileOutputStream);
        }
      }

      getCloudNet().deployTemplateInCluster(serviceTemplate,
        LocalTemplateStorageUtil.getLocalTemplateStorage()
          .toZipByteArray(serviceTemplate));

      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", "application/json")
        .body(new JsonDocument("success", true).toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext()
      ;

    } else {
      this.send404Response(context, "template not found!");
    }
  }

  @Override
  public void handleDelete(String path, IHttpContext context) throws Exception {
    if (!context.request().pathParameters().containsKey("prefix") || !context
      .request().pathParameters().containsKey("name")) {
      this.send400Response(context,
        "path parameter prefix or suffix doesn't exists");
      return;
    }

    ServiceTemplate serviceTemplate = createLocalTemplate(
      context.request().pathParameters().get("prefix"),
      context.request().pathParameters().get("name"));

    if (LocalTemplateStorageUtil.getLocalTemplateStorage()
      .has(serviceTemplate)) {
      File file = getFileByPath(path, serviceTemplate);

      if (file == null || !file.exists()) {
        this.send404Response(context,
          "file or directory '" + file.getName() + "' in template '"
            + serviceTemplate.getTemplatePath() + "' not found");
        return;
      }

      FileUtils.delete(file);
      getCloudNet().deployTemplateInCluster(serviceTemplate,
        LocalTemplateStorageUtil.getLocalTemplateStorage()
          .toZipByteArray(serviceTemplate));

      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", "application/json")
        .body(new JsonDocument("success", true).toByteArray())
        .context()
        .closeAfter(true)
        .cancelNext()
      ;

    } else {
      this.send404Response(context, "template not found!");
    }
  }

  /*= --------------------------------------------------------------------------------------- =*/

  private JsonDocument getFileEntry(File file) {
    Validate.checkNotNull(file);

    return new JsonDocument()
      .append("name", file.getName())
      .append("directory", file.isDirectory())
      .append("hidden", file.isHidden())
      .append("lastModified", file.lastModified())
      .append("canRead", file.canRead())
      .append("canWrite", file.canWrite())
      .append("length", file.length())
      ;
  }

  private File getFileByPath(String path, ServiceTemplate serviceTemplate) {
    String[] relativePathArray = path.split("/files");
    String relativePath =
      relativePathArray.length == 1 ? "." : relativePathArray[1].substring(1);
    return LocalTemplateStorageUtil.getFile(serviceTemplate, relativePath);
  }

  private ServiceTemplate createLocalTemplate(String prefix, String name) {
    return new ServiceTemplate(prefix, name,
      LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);
  }

  private void send404Response(IHttpContext context, String reason) {
    Validate.checkNotNull(context);
    Validate.checkNotNull(reason);

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