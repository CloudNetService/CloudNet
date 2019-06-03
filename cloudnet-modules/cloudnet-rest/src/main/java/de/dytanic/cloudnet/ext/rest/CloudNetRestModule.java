package de.dytanic.cloudnet.ext.rest;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerAuthentication;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerCluster;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerCommand;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerDatabase;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerGroups;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerLocalTemplate;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerLocalTemplateFileSystem;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerLogout;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerModules;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerPing;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerServices;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerShowOpenAPI;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerStatus;
import de.dytanic.cloudnet.ext.rest.http.V1HttpHandlerTasks;
import de.dytanic.cloudnet.ext.rest.http.V1SecurityProtectionHttpHandler;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

public final class CloudNetRestModule extends NodeCloudNetModule {

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void initHttpHandlers() {
    getHttpServer()
        .registerHandler("/api/v1", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerShowOpenAPI())
        //HttpHandler API implementation
        .registerHandler("/api/v1/*", IHttpHandler.PRIORITY_HIGH,
            new V1SecurityProtectionHttpHandler())
        .registerHandler("/api/v1/auth", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerAuthentication())
        .registerHandler("/api/v1/logout", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerLogout())
        .registerHandler("/api/v1/ping", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerPing("cloudnet.http.v1.ping"))
        .registerHandler("/api/v1/status", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerStatus("cloudnet.http.v1.status"))
        .registerHandler("/api/v1/command", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerCommand("cloudnet.http.v1.command"))
        .registerHandler("/api/v1/modules", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerModules("cloudnet.http.v1.modules"))
        .registerHandler("/api/v1/cluster", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerCluster("cloudnet.http.v1.cluster"))
        .registerHandler("/api/v1/cluster/{node}", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerCluster("cloudnet.http.v1.cluster"))
        .registerHandler("/api/v1/services", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerServices("cloudnet.http.v1.services"))
        .registerHandler("/api/v1/services/{uuid}",
            IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerServices("cloudnet.http.v1.services"))
        .registerHandler("/api/v1/services/{uuid}/{operation}",
            IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerServices("cloudnet.http.v1.services.operation"))
        .registerHandler("/api/v1/tasks", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerTasks("cloudnet.http.v1.tasks"))
        .registerHandler("/api/v1/tasks/{name}", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerTasks("cloudnet.http.v1.tasks"))
        .registerHandler("/api/v1/groups", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerGroups("cloudnet.http.v1.groups"))
        .registerHandler("/api/v1/groups/{name}", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerGroups("cloudnet.http.v1.groups"))
        .registerHandler("/api/v1/db/{name}", IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerDatabase("cloudnet.http.v1.database"))
        .registerHandler("/api/v1/db/{name}/{key}",
            IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerDatabase("cloudnet.http.v1.database"))
        .registerHandler("/api/v1/local_templates",
            IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerLocalTemplate("cloudnet.http.v1.lt.list"))
        .registerHandler("/api/v1/local_templates/{prefix}/{name}",
            IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerLocalTemplate("cloudnet.http.v1.lt.template"))
        .registerHandler("/api/v1/local_templates/{prefix}/{name}/files",
            IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerLocalTemplateFileSystem(
                "cloudnet.http.v1.lt.files"))
        .registerHandler("/api/v1/local_templates/{prefix}/{name}/files/*",
            IHttpHandler.PRIORITY_NORMAL,
            new V1HttpHandlerLocalTemplateFileSystem(
                "cloudnet.http.v1.lt.files"))
    ;
  }
}