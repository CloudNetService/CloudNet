/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.rest;

import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpHandler;
import eu.cloudnetservice.cloudnet.driver.network.http.content.ContentStreamProvider;
import eu.cloudnetservice.cloudnet.driver.network.http.content.StaticContentHttpHandler;
import eu.cloudnetservice.cloudnet.node.Node;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerAuthorization;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerCluster;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerDatabase;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerGroups;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerModule;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerNode;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerPermission;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerService;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerServiceVersionProvider;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerSession;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTasks;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTemplate;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTemplateStorages;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerWebSocketTicket;

public final class CloudNetRestModule extends DriverModule {

  @ModuleTask(order = 120, event = ModuleLifeCycle.STARTED)
  public void initHttpHandlers() {
    Node.instance().httpServer()
      // v2 openapi specification
      .registerHandler("/api/v2/documentation", HttpHandler.PRIORITY_NORMAL, new StaticContentHttpHandler(
        ContentStreamProvider.classLoader(this.classLoader(), "documentation")
      ))
      .registerHandler("/api/v2/documentation/*", HttpHandler.PRIORITY_LOW, new StaticContentHttpHandler(
        ContentStreamProvider.classLoader(this.classLoader(), "documentation")
      ))
      // v2 rest auth
      .registerHandler("/api/v2/auth", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerAuthorization())
      .registerHandler("/api/v2/wsTicket", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerWebSocketTicket("http.v2.ws_ticket"))
      // v2 session management
      .registerHandler("/api/v2/session/*", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerSession())
      // v2 node handling
      .registerHandler("/api/v2/node", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerNode("http.v2.node"))
      .registerHandler("/api/v2/node/*", HttpHandler.PRIORITY_LOW, new V2HttpHandlerNode("http.v2.node"))
      // v2 cluster
      .registerHandler("/api/v2/cluster", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerCluster("http.v2.cluster"))
      .registerHandler("/api/v2/cluster/{node}", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerCluster("http.v2.cluster"))
      .registerHandler("/api/v2/cluster/{node}/command", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerCluster("http.v2.cluster"))
      // v2 database
      .registerHandler("/api/v2/database", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerDatabase("http.v2.database"))
      .registerHandler("/api/v2/database/{name}", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerDatabase("http.v2.database"))
      .registerHandler("/api/v2/database/{name}/*", HttpHandler.PRIORITY_LOW,
        new V2HttpHandlerDatabase("http.v2.database"))
      // v2 groups
      .registerHandler("/api/v2/group", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerGroups("http.v2.groups"))
      .registerHandler("/api/v2/group/{group}", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerGroups("http.v2.groups"))
      .registerHandler("/api/v2/group/{group}/*", HttpHandler.PRIORITY_LOW, new V2HttpHandlerGroups("http.v2.groups"))
      // v2 permissions
      .registerHandler("/api/v2/permission/group/", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/user/", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/group/{group}", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/user/{user}", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/group/{group}/*", HttpHandler.PRIORITY_LOW,
        new V2HttpHandlerPermission("http.v2.permission"))
      .registerHandler("/api/v2/permission/user/{user}/*", HttpHandler.PRIORITY_LOW,
        new V2HttpHandlerPermission("http.v2.permission"))
      // v2 tasks
      .registerHandler("/api/v2/task", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerTasks("http.v2.tasks"))
      .registerHandler("/api/v2/task/{task}", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerTasks("http.v2.tasks"))
      .registerHandler("/api/v2/task/{task}/*", HttpHandler.PRIORITY_LOW, new V2HttpHandlerTasks("http.v2.tasks"))
      // v2 services
      .registerHandler("/api/v2/service", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerService("http.v2.services"))
      .registerHandler("/api/v2/service/{identifier}", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerService("http.v2.services"))
      .registerHandler("/api/v2/service/{identifier}/*", HttpHandler.PRIORITY_LOW,
        new V2HttpHandlerService("http.v2.services"))
      // v2 template storage management
      .registerHandler("/api/v2/templatestorage", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerTemplateStorages("http.v2.template.storage"))
      .registerHandler("/api/v2/templatestorage/{storage}/*", HttpHandler.PRIORITY_LOW,
        new V2HttpHandlerTemplateStorages("http.v2.template.storage"))
      // v2 template management
      .registerHandler("/api/v2/template/{storage}/{prefix}/{name}/*", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerTemplate("http.v2.template"))
      // v2 server version management
      .registerHandler("/api/v2/serviceversion", HttpHandler.PRIORITY_NORMAL,
        new V2HttpHandlerServiceVersionProvider("http.v2.service.provider"))
      .registerHandler("/api/v2/serviceversion/{version}", HttpHandler.PRIORITY_LOW,
        new V2HttpHandlerServiceVersionProvider("http.v2.service.provider"))
      // v2 module management
      .registerHandler("/api/v2/module", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerModule("http.v2.module"))
      .registerHandler("/api/v2/module/{name}", HttpHandler.PRIORITY_NORMAL, new V2HttpHandlerModule("http.v2.module"))
      .registerHandler("/api/v2/module/{name}/*", HttpHandler.PRIORITY_LOW, new V2HttpHandlerModule("http.v2.module"));
  }
}
