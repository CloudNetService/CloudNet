/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.http.HttpServer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.rest.scope.DefaultUserManagement;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerAuthorization;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerCluster;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerDatabase;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerDocumentation;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerGroup;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerModule;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerNode;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerService;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerServiceVersionProvider;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerSession;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTask;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTemplate;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTemplateStorage;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import eu.cloudnetservice.node.http.RestUserManagement;
import eu.cloudnetservice.node.http.V2HttpAuthentication;
import eu.cloudnetservice.node.http.annotation.SecurityAnnotationExtension;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class CloudNetRestModule extends DriverModule {

  @ModuleTask
  public void installSecurityExtensions(
    @NonNull HttpServer httpServer,
    @NonNull V2HttpAuthentication authentication,
    @NonNull SecurityAnnotationExtension securityAnnotationExtension
  ) {
    securityAnnotationExtension.install(httpServer.annotationParser(), authentication);
  }

  @ModuleTask(order = 127)
  public void registerUserManagement(
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull NodeDatabaseProvider databaseProvider
  ) {
    var restUserManagement = new DefaultUserManagement(databaseProvider);
    serviceRegistry.registerProvider(RestUserManagement.class, "RestUserManagement", restUserManagement);
  }

  @ModuleTask(order = 120)
  public void registerCommand(@NonNull CommandProvider commandProvider) {
    commandProvider.register(RestCommand.class);
  }

  @ModuleTask
  public void registerHandlers(@NonNull HttpServer httpServer) {
    httpServer.annotationParser()
      .parseAndRegister(V2HttpHandlerAuthorization.class)
      .parseAndRegister(V2HttpHandlerCluster.class)
      .parseAndRegister(V2HttpHandlerDatabase.class)
      .parseAndRegister(V2HttpHandlerDocumentation.class)
      .parseAndRegister(V2HttpHandlerGroup.class)
      .parseAndRegister(V2HttpHandlerModule.class)
      .parseAndRegister(V2HttpHandlerModule.class)
      .parseAndRegister(V2HttpHandlerNode.class)
      .parseAndRegister(V2HttpHandlerService.class)
      .parseAndRegister(V2HttpHandlerServiceVersionProvider.class)
      .parseAndRegister(V2HttpHandlerSession.class)
      .parseAndRegister(V2HttpHandlerTask.class)
      .parseAndRegister(V2HttpHandlerTemplate.class)
      .parseAndRegister(V2HttpHandlerTemplateStorage.class);
  }
}
