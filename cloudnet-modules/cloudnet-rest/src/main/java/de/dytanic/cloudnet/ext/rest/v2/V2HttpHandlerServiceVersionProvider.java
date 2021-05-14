package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.rest.RestUtils;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;
import de.dytanic.cloudnet.template.install.ServiceVersionProvider;
import de.dytanic.cloudnet.template.install.ServiceVersionType;

import java.io.IOException;

public class V2HttpHandlerServiceVersionProvider extends V2HttpHandler {

    public V2HttpHandlerServiceVersionProvider(String requiredPermission) {
        super(requiredPermission, "GET", "POST");
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) throws Exception {
        if (context.request().method().equalsIgnoreCase("GET")) {
            if (path.endsWith("/serviceversion")) {
                this.handleVersionListRequest(context);
            } else if (path.contains("/load")) {
                this.handleVersionLoadRequest(context);
            } else {
                this.handleVersionRequest(context);
            }
        } else if (context.request().method().equalsIgnoreCase("POST")) {
            this.handleVersionAddRequest(context);
        }
    }

    protected void handleVersionListRequest(IHttpContext context) {
        this.ok(context)
                .body(this.success().append("versions", this.getVersionProvider().getServiceVersionTypes()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleVersionRequest(IHttpContext context) {
        String version = context.request().pathParameters().get("version");
        if (version == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing version identifier").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        ServiceVersionType serviceVersion = this.getVersionProvider().getServiceVersionType(version).orElse(null);
        if (serviceVersion == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Unknown service version").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        this.ok(context)
                .body(this.success().append("version", serviceVersion).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleVersionLoadRequest(IHttpContext context) {
        String url = RestUtils.getFirst(context.request().queryParameters().get("url"), null);
        if (url == null) {
            this.getVersionProvider().loadDefaultVersionTypes();
        } else {
            try {
                if (!this.getVersionProvider().loadServiceVersionTypes(url)) {
                    this.ok(context).body(this.failure().toByteArray()).context().closeAfter(true).cancelNext();
                    return;
                }
            } catch (IOException exception) {
                this.badRequest(context)
                        .body(this.failure().append("reason", "Unable to load versions from provided url").toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
                return;
            }
        }

        this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
    }

    protected void handleVersionAddRequest(IHttpContext context) {
        ServiceVersionType type = this.body(context.request()).toInstanceOf(ServiceVersionType.class);
        if (type == null || type.getName() == null || type.getVersions() == null || type.getTargetEnvironment() == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing specific data").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        this.getVersionProvider().registerServiceVersionType(type);
        this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
    }

    protected ServiceVersionProvider getVersionProvider() {
        return this.getCloudNet().getServiceVersionProvider();
    }
}
