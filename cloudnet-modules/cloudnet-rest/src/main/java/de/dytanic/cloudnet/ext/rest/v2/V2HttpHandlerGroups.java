package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;

import java.util.function.Consumer;

public class V2HttpHandlerGroups extends V2HttpHandler {

    public V2HttpHandlerGroups(String requiredPermission) {
        super(requiredPermission, "GET", "POST", "DELETE");
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
        if (context.request().method().equalsIgnoreCase("GET")) {
            if (path.endsWith("/group")) {
                this.handleGroupListRequest(context);
            } else if (path.endsWith("/exists")) {
                this.handleGroupExistsRequest(context);
            } else {
                this.handleGroupRequest(context);
            }
        } else if (context.request().method().equalsIgnoreCase("POST")) {
            this.handleCreateGroupRequest(context);
        } else if (context.request().method().equalsIgnoreCase("DELETE")) {
            this.handleDeleteGroupRequest(context);
        }
    }

    protected void handleGroupListRequest(IHttpContext context) {
        this.ok(context)
                .body(this.success().append("groups", this.getGroupProvider().getGroupConfigurations()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleGroupExistsRequest(IHttpContext context) {
        this.handleWithGroupContext(context, name -> this.ok(context)
                .body(this.success().append("result", this.getGroupProvider().isGroupConfigurationPresent(name)).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext()
        );
    }

    protected void handleGroupRequest(IHttpContext context) {
        this.handleWithGroupContext(context, name -> {
            GroupConfiguration configuration = this.getGroupProvider().getGroupConfiguration(name);
            if (configuration == null) {
                this.ok(context)
                        .body(this.failure().append("reason", "Unknown configuration").toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            } else {
                this.ok(context)
                        .body(this.success().append("group", configuration).toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            }
        });
    }

    protected void handleCreateGroupRequest(IHttpContext context) {
        GroupConfiguration configuration = this.body(context.request()).toInstanceOf(GroupConfiguration.class);
        if (configuration == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing configuration").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        this.getGroupProvider().addGroupConfiguration(configuration);
        this.ok(context)
                .body(this.success().toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleDeleteGroupRequest(IHttpContext context) {
        this.handleWithGroupContext(context, name -> {
            if (this.getGroupProvider().isGroupConfigurationPresent(name)) {
                this.getGroupProvider().removeGroupConfiguration(name);
                this.ok(context)
                        .body(this.success().toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            } else {
                this.response(context, HttpResponseCode.HTTP_GONE)
                        .body(this.failure().append("reason", "No such group").toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            }
        });
    }

    protected void handleWithGroupContext(IHttpContext context, Consumer<String> handler) {
        String groupName = context.request().pathParameters().get("group");
        if (groupName == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing group parameter").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        } else {
            handler.accept(groupName);
        }
    }

    protected GroupConfigurationProvider getGroupProvider() {
        return this.getCloudNet().getGroupConfigurationProvider();
    }
}
