package de.dytanic.cloudnet.ext.rest.http;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.http.V1HttpHandler;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.TemplateStorageUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

public final class V1HttpHandlerLocalTemplateFileSystem extends V1HttpHandler {

    public V1HttpHandlerLocalTemplateFileSystem(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "GET, DELETE, POST");
    }

    private boolean validateTemplate(IHttpContext context) {
        if (!context.request().pathParameters().containsKey("prefix") || !context.request().pathParameters().containsKey("name")) {
            this.send400Response(context, "path parameter prefix or suffix doesn't exists");
            return false;
        }
        return true;
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        if (!this.validateTemplate(context)) {
            return;
        }

        ServiceTemplate serviceTemplate = this.createLocalTemplate(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));

        if (TemplateStorageUtil.getLocalTemplateStorage().has(serviceTemplate)) {
            Path file = this.getFileByPath(path, serviceTemplate);

            if (Files.notExists(file)) {
                this.send404Response(context, "file '" + file.getFileName() + "' in template '" + serviceTemplate.getTemplatePath() + "' not found");
                return;
            }

            if (Files.isDirectory(file)) {
                Collection<JsonDocument> documents = new ArrayList<>();
                FileUtils.walkFileTree(file, (root, current) -> {
                    try {
                        documents.add(this.getFileEntry(current));
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });

                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_OK)
                        .header("Content-Type", "application/json")
                        .body(GSON.toJson(documents))
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            } else {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_OK)
                        .header("Content-Type", "application/octet-stream")
                        .header("Content-Disposition", "attachment; filename=\"" + file.getFileName() + "\"")
                        .body(Files.readAllBytes(file))
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            }

        } else {
            this.send404Response(context, "template not found!");
        }
    }

    @Override
    public void handlePost(String path, IHttpContext context) throws Exception {
        if (!this.validateTemplate(context)) {
            return;
        }

        ServiceTemplate serviceTemplate = this.createLocalTemplate(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));

        if (TemplateStorageUtil.getLocalTemplateStorage().has(serviceTemplate)) {
            Path file = this.getFileByPath(path, serviceTemplate);

            if (Files.notExists(file)) {
                FileUtils.createDirectoryReported(file.getParent());
                try (ByteArrayInputStream in = new ByteArrayInputStream(context.request().body()); OutputStream out = Files.newOutputStream(file)) {
                    FileUtils.copy(in, out);
                }
            }

            InputStream inputStream = TemplateStorageUtil.getLocalTemplateStorage().zipTemplate(serviceTemplate);
            if (inputStream == null) {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_INTERNAL_ERROR)
                        .header("Content-Type", "application/json")
                        .body(new JsonDocument("success", false).toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
                return;
            }

            this.getCloudNet().deployTemplateInCluster(serviceTemplate, inputStream);

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
    public void handleDelete(String path, IHttpContext context) throws Exception {
        if (!this.validateTemplate(context)) {
            return;
        }

        ServiceTemplate serviceTemplate = this.createLocalTemplate(context.request().pathParameters().get("prefix"), context.request().pathParameters().get("name"));

        if (TemplateStorageUtil.getLocalTemplateStorage().has(serviceTemplate)) {
            Path file = this.getFileByPath(path, serviceTemplate);

            if (Files.notExists(file)) {
                this.send404Response(context, "file or directory '" + file.getFileName() + "' in template '" + serviceTemplate.getTemplatePath() + "' not found");
                return;
            }

            FileUtils.delete(file);
            InputStream inputStream = TemplateStorageUtil.getLocalTemplateStorage().zipTemplate(serviceTemplate);
            if (inputStream == null) {
                context
                        .response()
                        .statusCode(HttpResponseCode.HTTP_INTERNAL_ERROR)
                        .header("Content-Type", "application/json")
                        .body(new JsonDocument("success", false).toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
                return;
            }

            this.getCloudNet().deployTemplateInCluster(serviceTemplate, inputStream);

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


    private JsonDocument getFileEntry(Path file) throws IOException {
        Preconditions.checkNotNull(file);

        return new JsonDocument()
                .append("name", file.getFileName().toString())
                .append("directory", Files.isDirectory(file))
                .append("hidden", Files.isHidden(file))
                .append("lastModified", Files.getLastModifiedTime(file))
                .append("canRead", Files.isReadable(file))
                .append("canWrite", Files.isWritable(file))
                .append("length", Files.size(file));
    }

    private Path getFileByPath(String path, ServiceTemplate serviceTemplate) {
        String[] relativePathArray = path.split("/files");
        String relativePath = relativePathArray.length == 1 ? "." : relativePathArray[1].substring(1);
        return TemplateStorageUtil.getPath(serviceTemplate, relativePath);
    }

    private ServiceTemplate createLocalTemplate(String prefix, String name) {
        return new ServiceTemplate(prefix, name, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);
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