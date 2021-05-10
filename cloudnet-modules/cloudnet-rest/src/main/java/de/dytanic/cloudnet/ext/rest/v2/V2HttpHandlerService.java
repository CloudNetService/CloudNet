package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;

import java.util.UUID;
import java.util.function.Consumer;

public class V2HttpHandlerService extends V2HttpHandler {

    public V2HttpHandlerService(String requiredPermission) {
        super(requiredPermission, "GET", "POST", "DELETE");
    }

    // d     d        d                                d
    // GET   GET      GET                      DELETE  GET      POST                               GET                           GET     GET        GET
    // list, service, start/stop/restart/kill, delete, command, add template/deployment/inclusion, include templates/inclusions, deploy, log lines, log web socket

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
        if (context.request().method().equalsIgnoreCase("GET")) {
            if (path.endsWith("/service")) {
                this.handleListServicesRequest(context);
            } else if (path.endsWith("/start") || path.endsWith("/stop") || path.endsWith("/restart") || path.endsWith("/kill")) {
                this.handleServiceStateUpdateRequest(path, context);
            } else if (path.endsWith("/command")) {
                this.handleServiceCommandRequest(context);
            } else if (path.endsWith("/include")) {

            }
        } else if (context.request().method().equalsIgnoreCase("POST")) {

        } else if (context.request().method().equalsIgnoreCase("DELETE")) {

        }
    }

    protected void handleListServicesRequest(IHttpContext context) {
        this.ok(context)
                .body(this.success().append("services", this.getGeneralServiceProvider().getCloudServices()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleServiceStateUpdateRequest(String path, IHttpContext context) {
        this.handleWithServiceContext(context, service -> {
            if (path.endsWith("/start")) {
                service.provider().start();
            } else if (path.endsWith("/stop")) {
                service.provider().stop();
            } else if (path.endsWith("/restart")) {
                service.provider().restart();
            } else if (path.endsWith("/kill")) {
                service.provider().kill();
            }

            this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
        });
    }

    protected void handleServiceCommandRequest(IHttpContext context) {
        this.handleWithServiceContext(context, service -> {
            String commandLine = this.body(context.request()).getString("command");
            if (commandLine == null) {
                this.badRequest(context)
                        .body(this.failure().append("reason", "Missing command line").toByteArray())
                        .context()
                        .closeAfter(true)
                        .cancelNext();
            } else {
                service.provider().runCommand(commandLine);
                this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
            }
        });
    }

    protected void handleIncludeRequest(IHttpContext context) {
        this.handleWithServiceContext(context, service -> {

        });
    }

    protected void handleWithServiceContext(IHttpContext context, Consumer<ServiceInfoSnapshot> handler) {
        String identifier = context.request().pathParameters().get("identifier");
        if (identifier == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing service identifier").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }
        // try to find a matching service
        ServiceInfoSnapshot serviceInfoSnapshot;
        try {
            // try to parse a unique id from that
            UUID serviceId = UUID.fromString(identifier);
            serviceInfoSnapshot = this.getServiceById(serviceId);
        } catch (Exception exception) {
            serviceInfoSnapshot = this.getServiceByName(identifier);
        }
        // check if the snapshot is present before applying to the handler
        if (serviceInfoSnapshot == null) {
            this.ok(context)
                    .body(this.failure().append("reason", "No service with provided uniqueId/name").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }
        // post to handler
        handler.accept(serviceInfoSnapshot);
    }

    protected GeneralCloudServiceProvider getGeneralServiceProvider() {
        return this.getCloudNet().getCloudServiceProvider();
    }

    protected CloudServiceFactory getServiceFactory() {
        return this.getCloudNet().getCloudServiceFactory();
    }

    protected ServiceInfoSnapshot getServiceByName(String name) {
        return this.getGeneralServiceProvider().getCloudServiceByName(name);
    }

    protected ServiceInfoSnapshot getServiceById(UUID uniqueID) {
        return this.getGeneralServiceProvider().getCloudService(uniqueID);
    }
}
