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

package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.util.stream.Collectors;

public final class V1HttpHandlerModules extends V1HttpHandler {

  public V1HttpHandlerModules(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    this.sendOptions(context, "OPTIONS, GET");
  }

  @Override
  public void handleGet(String path, IHttpContext context) {
    if (context.request().pathParameters().containsKey("name")) {
      context
        .response()
        .header("Content-Type", "application/json")
        .statusCode(HttpResponseCode.HTTP_OK)
        .body(GSON.toJson(CloudNetDriver.getInstance().getModuleProvider().getModules().stream()
          .filter(moduleWrapper -> context.request().pathParameters().get("name")
            .contains(moduleWrapper.getModuleConfiguration().getName()))
          .collect(Collectors.toList())))
      ;
    } else {
      context
        .response()
        .header("Content-Type", "application/json")
        .statusCode(200)
        .body(GSON.toJson(CloudNetDriver.getInstance().getModuleProvider().getModules().stream()
          .map(IModuleWrapper::getModuleConfiguration)
          .filter(moduleConfiguration -> {
            if (context.request().queryParameters().containsKey("group") &&
              !this.containsStringElementInCollection(context.request().queryParameters().get("group"),
                moduleConfiguration.getGroup())) {
              return false;
            }

            if (context.request().queryParameters().containsKey("name") &&
              !this.containsStringElementInCollection(context.request().queryParameters().get("name"),
                moduleConfiguration.getName())) {
              return false;
            }

            return !context.request().queryParameters().containsKey("version") ||
              this.containsStringElementInCollection(context.request().queryParameters().get("version"),
                moduleConfiguration.getVersion());
          }).collect(Collectors.toList())))
        .context()
        .closeAfter(true)
        .cancelNext()
      ;
    }
  }
}
