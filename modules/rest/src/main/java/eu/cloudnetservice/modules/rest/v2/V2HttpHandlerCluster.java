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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.http.HttpContext;
import eu.cloudnetservice.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.driver.network.http.annotation.HttpRequestHandler;
import eu.cloudnetservice.driver.network.http.annotation.RequestBody;
import eu.cloudnetservice.driver.network.http.annotation.RequestPathParam;
import eu.cloudnetservice.node.cluster.LocalNodeServer;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.http.V2HttpHandler;
import eu.cloudnetservice.node.http.annotation.ApplyHeaders;
import eu.cloudnetservice.node.http.annotation.BearerAuth;
import eu.cloudnetservice.node.http.annotation.HandlerPermission;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@HandlerPermission("http.v2.cluster")
@ApplyHeaders
public final class V2HttpHandlerCluster extends V2HttpHandler {

  private final Configuration configuration;
  private final NodeServerProvider nodeServerProvider;

  @Inject
  public V2HttpHandlerCluster(@NonNull Configuration configuration, @NonNull NodeServerProvider nodeServerProvider) {
    this.configuration = configuration;
    this.nodeServerProvider = nodeServerProvider;
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/cluster")
  private void handleNodeListRequest(@NonNull HttpContext context) {
    var nodes = this.nodeServerProvider.nodeServers().stream()
      .map(this::createNodeInfoDocument)
      .toList();

    this.ok(context)
      .body(this.success().append("nodes", nodes).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/cluster/{node}")
  private void handleNodeRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("node") String node) {
    var server = this.nodeServerProvider.node(node);
    if (server != null) {
      this.ok(context)
        .body(this.success().append("node", this.createNodeInfoDocument(server)).toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.response(context, HttpResponseCode.NOT_FOUND)
        .body(this.failure().append("reason", "No such node found").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    }
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/cluster/{node}/command", methods = "POST")
  private void handleNodeCommandRequest(
    @NonNull HttpContext context,
    @NonNull @RequestPathParam("node") String node,
    @NonNull @RequestBody Document body
  ) {
    var nodeServer = this.nodeServerProvider.node(node);
    var commandLine = body.getString("command");
    if (commandLine == null || nodeServer == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", nodeServer == null ? "Unknown node server" : "Missing command line")
          .toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    var result = nodeServer.sendCommandLine(commandLine);
    this.ok(context)
      .body(this.success().append("result", result).toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/cluster", methods = "POST")
  private void handleNodeCreateRequest(@NonNull HttpContext context, @NonNull @RequestBody Document body) {
    var server = body.toInstanceOf(NetworkClusterNode.class);
    if (server == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing node server information").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    if (this.nodeServerProvider.node(server.uniqueId()) != null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "The node server is already registered").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    this.configuration.clusterConfig().nodes().add(server);
    this.configuration.save();

    this.nodeServerProvider.registerNodes(this.configuration.clusterConfig());
    this.response(context, HttpResponseCode.CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext(true);
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/cluster/{node}", methods = "DELETE")
  private void handleNodeDeleteRequest(@NonNull HttpContext context, @NonNull @RequestPathParam("node") String node) {
    var removed = this.configuration.clusterConfig().nodes()
      .removeIf(clusterNode -> clusterNode.uniqueId().equals(node));
    if (removed) {
      this.configuration.save();
      this.nodeServerProvider.registerNodes(this.configuration.clusterConfig());

      this.response(context, HttpResponseCode.OK)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.response(context, HttpResponseCode.NOT_FOUND)
        .body(this.failure().append("reason", "No node with that unique id present").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    }
  }

  @BearerAuth
  @HttpRequestHandler(paths = "/api/v2/cluster", methods = "PUT")
  private void handleNodeUpdateRequest(@NonNull HttpContext context, @NonNull @RequestBody Document body) {
    var server = body.toInstanceOf(NetworkClusterNode.class);
    if (server == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing node server information").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
      return;
    }

    var registered = this.configuration.clusterConfig().nodes()
      .stream()
      .filter(node -> node.uniqueId().equals(server.uniqueId()))
      .findFirst()
      .orElse(null);
    if (registered == null) {
      this.response(context, HttpResponseCode.NOT_FOUND)
        .body(this.failure().append("reason", "No node with that unique id present").toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    } else {
      this.configuration.clusterConfig().nodes().remove(registered);
      this.configuration.clusterConfig().nodes().add(server);

      this.configuration.save();
      this.nodeServerProvider.registerNodes(this.configuration.clusterConfig());

      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext(true);
    }
  }

  private @NonNull Document createNodeInfoDocument(@NonNull NodeServer node) {
    return Document.newJsonDocument()
      .append("node", node.info())
      .append("state", node.state())
      .append("head", node.head())
      .append("local", node instanceof LocalNodeServer)
      .append("nodeInfoSnapshot", node.nodeInfoSnapshot());
  }
}
