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

package de.dytanic.cloudnet.ext.rest;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.content.ContentStreamProvider;
import de.dytanic.cloudnet.driver.network.http.content.StaticContentHttpHandler;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerAuthorization;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerCluster;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerDatabase;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerGroups;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerModule;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerNode;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerPermission;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerService;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerServiceVersionProvider;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerSession;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerTasks;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerTemplate;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerTemplateStorages;
import de.dytanic.cloudnet.ext.rest.v2.V2HttpHandlerWebSocketTicket;
import de.dytanic.cloudnet.http.AccessControlConfiguration;

public final class CloudNetRestModule extends DriverModule {

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void loadConfiguration() {
    var corsPolicy = this.readConfig().getString("corsPolicy", "*");
    var accessControlMaxAge = this.readConfig().getInt("accessControlMaxAge", 3600);

    AccessControlConfiguration.setDefaultConfiguration(new AccessControlConfiguration(corsPolicy, accessControlMaxAge));
  }

  @ModuleTask(order = 120, event = ModuleLifeCycle.STARTED)
  public void initHttpHandlers() {
    CloudNet.instance().httpServer()
      // v2 openapi specification
      .registerHandler("/api/v2/documentation", IHttpHandler.PRIORITY_NORMAL, new StaticContentHttpHandler(
        ContentStreamProvider.classLoader(this.classLoader(), "documentation")
      ))
      .registerHandler("/api/v2/documentation/*", IHttpHandler.PRIORITY_LOW, new StaticContentHttpHandler(
        ContentStreamProvider.classLoader(this.classLoader(), "documentation")
      ))
      // v2 rest auth
      .registerHandler("/api/v2/auth", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerAuthorization())
      .registerHandler("/api/v2/wsTicket", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerWebSocketTicket("http.v2.ws_ticket"))
      // v2 session management
      .registerHandler("/api/v2/session/*", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerSession())
      // v2 node handling
      .registerHandler("/api/v2/node", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerNode("http.v2.node"))
      .registerHandler("/api/v2/node/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerNode("http.v2.node"))
      // v2 cluster
      .registerHandler("/api/v2/cluster", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerCluster("http.v2.cluster"))
      .registerHandler("/api/v2/cluster/{node}", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerCluster("http.v2.cluster"))
      .registerHandler("/api/v2/cluster/{node}/command", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerCluster("http.v2.cluster"))
      // v2 database
      .registerHandler("/api/v2/database", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerDatabase("http.v2.database"))
      .registerHandler("/api/v2/database/{name}", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerDatabase("http.v2.database"))
      .registerHandler("/api/v2/database/{name}/*", IHttpHandler.PRIORITY_LOW,
        new V2HttpHandlerDatabase("http.v2.database"))
      // v2 groups
      .registerHandler("/api/v2/group", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerGroups("http.v2.groups"))
      .registerHandler("/api/v2/group/{group}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerGroups("http.v2.groups"))
      .registerHandler("/api/v2/group/{group}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerGroups("http.v2.groups"))
      // v2 permissions
      .registerHandler("/api/v2/permission/group/", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/user/", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/group/{group}", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/user/{user}", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/group/{group}/*", IHttpHandler.PRIORITY_LOW,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/user/{user}/*", IHttpHandler.PRIORITY_LOW,
        new V2HttpHandlerPermission("http.v2.permission"))
      // v2 tasks
      .registerHandler("/api/v2/task", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerTasks("http.v2.tasks"))
      .registerHandler("/api/v2/task/{task}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerTasks("http.v2.tasks"))
      .registerHandler("/api/v2/task/{task}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerTasks("http.v2.tasks"))
      // v2 services
      .registerHandler("/api/v2/service", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerService("http.v2.services"))
      .registerHandler("/api/v2/service/{identifier}", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerService("http.v2.services"))
      .registerHandler("/api/v2/service/{identifier}/*", IHttpHandler.PRIORITY_LOW,
        new V2HttpHandlerService("http.v2.services"))
      // v2 template storage management
      .registerHandler("/api/v2/templateStorage", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerTemplateStorages("http.v2.template.storage"))
      .registerHandler("/api/v2/templateStorage/{storage}/*", IHttpHandler.PRIORITY_LOW,
        new V2HttpHandlerTemplateStorages("http.v2.template.storage"))
      // v2 template management
      .registerHandler("/api/v2/template/{storage}/{prefix}/{name}/*", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerTemplate("http.v2.template"))
      // v2 server version management
      .registerHandler("/api/v2/serviceVersion", IHttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerServiceVersionProvider("http.v2.service.provider"))
      .registerHandler("/api/v2/serviceVersion/{version}", IHttpHandler.PRIORITY_LOW,
        new V2HttpHandlerServiceVersionProvider("http.v2.service.provider"))
      // v2 module management
      .registerHandler("/api/v2/module", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerModule("http.v2.module"))
      .registerHandler("/api/v2/module/{name}", IHttpHandler.PRIORITY_NORMAL, new V2HttpHandlerModule("http.v2.module"))
      .registerHandler("/api/v2/module/{name}/*", IHttpHandler.PRIORITY_LOW, new V2HttpHandlerModule("http.v2.module"));
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    this.loadConfiguration();
  }
}
