package de.dytanic.cloudnet.ext.rest;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.http.HttpHandler;
import de.dytanic.cloudnet.ext.rest.http.*;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

public final class CloudNetRestModule extends NodeCloudNetModule {

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void initHttpHandlers() {
        getHttpServer()
                .registerHandler("/api/v1", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerShowOpenAPI())
                //HttpHandler API implementation
                .registerHandler("/api/v1/*", HttpHandler.PRIORITY_HIGH, new V1SecurityProtectionHttpHandler())
                .registerHandler("/api/v1/auth", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerAuthentication())
                .registerHandler("/api/v1/logout", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLogout())
                .registerHandler("/api/v1/ping", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerPing("cloudnet.http.v1.ping"))
                .registerHandler("/api/v1/status", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerStatus("cloudnet.http.v1.status"))
                .registerHandler("/api/v1/command", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerCommand("cloudnet.http.v1.command"))
                .registerHandler("/api/v1/modules", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerModules("cloudnet.http.v1.modules"))
                .registerHandler("/api/v1/cluster", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerCluster("cloudnet.http.v1.cluster"))
                .registerHandler("/api/v1/cluster/{node}", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerCluster("cloudnet.http.v1.cluster"))
                .registerHandler("/api/v1/services", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerServices("cloudnet.http.v1.services"))
                .registerHandler("/api/v1/services/{uuid}", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerServices("cloudnet.http.v1.services"))
                .registerHandler("/api/v1/services/{uuid}/{operation}", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerServices("cloudnet.http.v1.services.operation"))
                .registerHandler("/api/v1/tasks", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerTasks("cloudnet.http.v1.tasks"))
                .registerHandler("/api/v1/tasks/{name}", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerTasks("cloudnet.http.v1.tasks"))
                .registerHandler("/api/v1/groups", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerGroups("cloudnet.http.v1.groups"))
                .registerHandler("/api/v1/groups/{name}", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerGroups("cloudnet.http.v1.groups"))
                .registerHandler("/api/v1/db/{name}", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerDatabase("cloudnet.http.v1.database"))
                .registerHandler("/api/v1/db/{name}/{key}", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerDatabase("cloudnet.http.v1.database"))
                .registerHandler("/api/v1/local_templates", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLocalTemplate("cloudnet.http.v1.lt.list"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLocalTemplate("cloudnet.http.v1.lt.template"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}/files", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLocalTemplateFileSystem("cloudnet.http.v1.lt.files"))
                .registerHandler("/api/v1/local_templates/{prefix}/{name}/files/*", HttpHandler.PRIORITY_NORMAL, new V1HttpHandlerLocalTemplateFileSystem("cloudnet.http.v1.lt.files"))
        ;
    }
}