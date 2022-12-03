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

import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerAuthorization;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerCluster;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerDatabase;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerDocumentation;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerGroup;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerModule;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerNode;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerPermission;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerService;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerServiceVersionProvider;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerSession;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTask;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTemplate;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTemplateStorage;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.http.V2HttpAuthentication;
import eu.cloudnetservice.node.http.annotation.SecurityAnnotationExtension;

public final class CloudNetRestModule extends DriverModule {

  @ModuleTask(order = 120, event = ModuleLifeCycle.STARTED)
  public void initHttpHandlers() {
    //var authentication = new V2HttpAuthentication();
    var annotationParser = Node.instance().httpServer().annotationParser();

    // register the security annotation processor
    // TODO: SecurityAnnotationExtension.install(annotationParser, authentication);

    // register all handlers
    annotationParser
      //.parseAndRegister(new V2HttpHandlerAuthorization(authentication))
      .parseAndRegister(new V2HttpHandlerCluster())
      .parseAndRegister(new V2HttpHandlerDatabase())
      .parseAndRegister(new V2HttpHandlerDocumentation())
      .parseAndRegister(new V2HttpHandlerGroup())
      .parseAndRegister(new V2HttpHandlerModule())
      .parseAndRegister(new V2HttpHandlerModule())
      .parseAndRegister(new V2HttpHandlerNode())
      .parseAndRegister(new V2HttpHandlerPermission())
      .parseAndRegister(new V2HttpHandlerService())
      .parseAndRegister(new V2HttpHandlerServiceVersionProvider())
      .parseAndRegister(new V2HttpHandlerSession())
      .parseAndRegister(new V2HttpHandlerTask())
      .parseAndRegister(new V2HttpHandlerTemplate())
      .parseAndRegister(new V2HttpHandlerTemplateStorage());
  }
}
