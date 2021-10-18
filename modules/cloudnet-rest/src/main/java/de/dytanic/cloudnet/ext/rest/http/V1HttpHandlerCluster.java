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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;
import java.util.stream.Collectors;

public final class V1HttpHandlerCluster extends V1HttpHandler {

  public V1HttpHandlerCluster(String permission) {
    super(permission);
  }

  @Override
  public void handleOptions(String path, IHttpContext context) {
    this.sendOptions(context, "OPTIONS, GET");
  }

  @Override
  public void handleGet(String path, IHttpContext context) {
    if (context.request().pathParameters().containsKey("node")) {
      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", "application/json")
        .body(GSON.toJson(
          super.getCloudNet().getClusterNodeServerProvider().getNodeServers().stream()
            .filter(nodeServer -> nodeServer.getNodeInfo().getUniqueId().toLowerCase()
              .contains(context.request().pathParameters().get("node")))
            .map(nodeServer -> new JsonDocument()
              .append("node", nodeServer.getNodeInfo())
              .append("nodeInfoSnapshot", nodeServer.getNodeInfoSnapshot()))
            .collect(Collectors.toList())))
        .context()
        .closeAfter(true)
        .cancelNext()
      ;
    } else {
      context
        .response()
        .statusCode(HttpResponseCode.HTTP_OK)
        .header("Content-Type", "application/json")
        .body(GSON.toJson(
          super.getCloudNet().getClusterNodeServerProvider().getNodeServers().stream()
            .filter(nodeServer -> !context.request().queryParameters().containsKey("uniqueId") ||
              this.containsStringElementInCollection(context.request().queryParameters().get("uniqueId"),
                nodeServer.getNodeInfo().getUniqueId()))
            .map(nodeServer -> new JsonDocument()
              .append("node", nodeServer.getNodeInfo())
              .append("nodeInfoSnapshot", nodeServer.getNodeInfoSnapshot()))
            .collect(Collectors.toList())))
        .context()
        .closeAfter(true)
        .cancelNext()
      ;
    }
  }
}
