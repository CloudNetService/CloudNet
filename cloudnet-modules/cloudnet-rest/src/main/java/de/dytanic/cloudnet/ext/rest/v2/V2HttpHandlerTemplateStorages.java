package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class V2HttpHandlerTemplateStorages extends V2HttpHandler {

    public V2HttpHandlerTemplateStorages(String requiredPermission) {
        super(requiredPermission, "GET");
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) throws Exception {
        if (context.request().method().equalsIgnoreCase("GET")) {
            if (path.endsWith("/templatestorage")) {
                this.handleStorageListRequest(context);
            } else if (path.endsWith("/templates")) {
                this.handleTemplateListRequest(context);
            }
        }
    }

    protected void handleStorageListRequest(IHttpContext context) {
        this.ok(context)
                .body(this.success().append("storages", this.getCloudNet().getAvailableTemplateStorages().stream()
                        .map(INameable::getName).collect(Collectors.toList())).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleTemplateListRequest(IHttpContext context) {
        this.handleWithStorageContext(context, templateStorage -> this.ok(context)
                .body(this.success().append("templates", templateStorage.getTemplates()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext());
    }

    protected void handleWithStorageContext(IHttpContext context, Consumer<TemplateStorage> handler) {
        String storage = context.request().pathParameters().get("storage");
        if (storage == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing template storage in path params").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        TemplateStorage templateStorage = this.getCloudNet().getTemplateStorage(storage);
        if (templateStorage == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Unknown template storage").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        handler.accept(templateStorage);
    }
}
