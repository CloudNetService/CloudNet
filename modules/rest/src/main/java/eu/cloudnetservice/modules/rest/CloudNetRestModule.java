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
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerAuthorization;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerCluster;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerDatabase;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerDocumentation;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerGroup;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerModule;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerNode;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerPermission;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerPreflight;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerService;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerServiceVersionProvider;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerSession;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTask;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTemplate;
import eu.cloudnetservice.modules.rest.v2.V2HttpHandlerTemplateStorage;
import eu.cloudnetservice.node.http.V2HttpAuthentication;
import eu.cloudnetservice.node.http.annotation.HeaderAnnotationExtension;
import eu.cloudnetservice.node.http.annotation.SecurityAnnotationExtension;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class CloudNetRestModule extends DriverModule {

  @ModuleTask
  public void installExtensions(
    @NonNull HttpServer httpServer,
    @NonNull V2HttpAuthentication authentication,
    @NonNull SecurityAnnotationExtension securityAnnotationExtension,
    @NonNull HeaderAnnotationExtension headerAnnotationExtension
  ) {
    securityAnnotationExtension.install(httpServer.annotationParser(), authentication);
    headerAnnotationExtension.install(httpServer.annotationParser());
  }

  @ModuleTask
  public void registerHandlers(@NonNull HttpServer httpServer) {
    httpServer.annotationParser()
      .parseAndRegister(V2HttpHandlerPreflight.class)
      .parseAndRegister(V2HttpHandlerAuthorization.class)
      .parseAndRegister(V2HttpHandlerCluster.class)
      .parseAndRegister(V2HttpHandlerDatabase.class)
      .parseAndRegister(V2HttpHandlerDocumentation.class)
      .parseAndRegister(V2HttpHandlerGroup.class)
      .parseAndRegister(V2HttpHandlerModule.class)
      .parseAndRegister(V2HttpHandlerNode.class)
      .parseAndRegister(V2HttpHandlerPermission.class)
      .parseAndRegister(V2HttpHandlerService.class)
      .parseAndRegister(V2HttpHandlerServiceVersionProvider.class)
      .parseAndRegister(V2HttpHandlerSession.class)
      .parseAndRegister(V2HttpHandlerTask.class)
      .parseAndRegister(V2HttpHandlerTemplate.class)
      .parseAndRegister(V2HttpHandlerTemplateStorage.class);
  }
}
