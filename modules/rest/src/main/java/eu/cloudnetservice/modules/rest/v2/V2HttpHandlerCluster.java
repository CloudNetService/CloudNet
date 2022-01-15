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

package eu.cloudnetservice.modules.rest.v2;

import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpContext;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpResponseCode;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.ClusterNodeServerProvider;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServer;
import eu.cloudnetservice.cloudnet.node.http.HttpSession;
import eu.cloudnetservice.cloudnet.node.http.V2HttpHandler;
import java.util.Collection;
import java.util.stream.Collectors;

public class V2HttpHandlerCluster extends V2HttpHandler {

  public V2HttpHandlerCluster(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE", "PUT");
  }

  @Override
  protected void handleBearerAuthorized(@NonNull String path, @NonNull HttpContext context, @NonNull HttpSession ses) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (context.request().pathParameters().containsKey("node")) {
        // specific node was requested
        this.handleNodeRequest(context);
      } else {
        // a list of all nodes was requested
        this.handleNodeListRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      if (path.endsWith("/command")) {
        // a command should be executed on the node
        this.handleNodeCommandRequest(context);
      } else {
        // post is used for creation of nodes
        this.handleNodeCreateRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      // delete a cluster server from the configuration
      this.handleNodeDeleteRequest(context);
    } else if (context.request().method().equalsIgnoreCase("PUT")) {
      // put (modifies) an existing cluster node
      this.handleNodeUpdateRequest(context);
    }
  }

  protected void handleNodeRequest(HttpContext context) {
    var server = this.getNodeServer(context, true);
    if (server != null) {
      this.ok(context)
        .body(this.success().append("node", this.createNodeInfoDocument(server)).toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.response(context, HttpResponseCode.NOT_FOUND)
        .body(this.failure().append("reason", "No such node found").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected void handleNodeListRequest(HttpContext context) {
    Collection<JsonDocument> nodes = this.nodeProvider().nodeServers().stream()
      .map(this::createNodeInfoDocument)
      .collect(Collectors.toList());
    // add the local node info
    nodes.add(this.createNodeInfoDocument(this.nodeProvider().selfNode()));

    this.ok(context)
      .body(this.success().append("nodes", nodes).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleNodeCommandRequest(HttpContext context) {
    var nodeServer = this.getNodeServer(context, true);
    var commandLine = this.body(context.request()).getString("command");
    if (commandLine == null || nodeServer == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", nodeServer == null ? "Unknown node server" : "Missing command line")
          .toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var result = nodeServer.sendCommandLine(commandLine);
    this.ok(context)
      .body(this.success().append("result", result).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleNodeCreateRequest(HttpContext context) {
    var server = this.body(context.request()).toInstanceOf(NetworkClusterNode.class);
    if (server == null || server.listeners() == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing node server information").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    if (this.getNodeServer(server.uniqueId(), true) != null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "The node server is already registered").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var configuration = this.configuration();
    configuration.clusterConfig().nodes().add(server);
    configuration.save();

    this.nodeProvider().clusterServers(configuration.clusterConfig());

    this.response(context, HttpResponseCode.CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleNodeDeleteRequest(HttpContext context) {
    var uniqueId = context.request().pathParameters().get("node");
    if (uniqueId == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "No node unique id provided").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var removed = this.configuration().clusterConfig().nodes().removeIf(
      node -> node.uniqueId().equals(uniqueId));
    if (removed) {
      this.configuration().save();
      this.nodeProvider().clusterServers(this.configuration().clusterConfig());

      this.response(context, HttpResponseCode.OK)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.response(context, HttpResponseCode.NOT_FOUND)
        .body(this.failure().append("reason", "No node with that unique id present").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected void handleNodeUpdateRequest(HttpContext context) {
    var server = this.body(context.request()).toInstanceOf(NetworkClusterNode.class);
    if (server == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing node server information").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var registered = this.configuration().clusterConfig().nodes()
      .stream()
      .filter(node -> node.uniqueId().equals(server.uniqueId()))
      .findFirst()
      .orElse(null);
    if (registered == null) {
      this.response(context, HttpResponseCode.NOT_FOUND)
        .body(this.failure().append("reason", "No node with that unique id present").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      //TODO: registered.setListeners(server.getListeners());
      registered.properties().append(server.properties());
      this.configuration().save();
      this.nodeProvider().clusterServers(this.configuration().clusterConfig());

      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected JsonDocument createNodeInfoDocument(NodeServer node) {
    return JsonDocument.newDocument("node", node.nodeInfo())
      .append("available", node.available())
      .append("head", node.headNode())
      .append("nodeInfoSnapshot", node.nodeInfoSnapshot());
  }

  protected NodeServer getNodeServer(HttpContext context, boolean includeLocal) {
    var nodeName = context.request().pathParameters().get("node");
    return nodeName == null ? null : this.getNodeServer(nodeName, includeLocal);
  }

  protected NodeServer getNodeServer(String nodeName, boolean includeLocal) {
    NodeServer server = this.nodeProvider().nodeServer(nodeName);
    if (server == null && includeLocal && nodeName.equals(CloudNet.instance().componentName())) {
      server = this.nodeProvider().selfNode();
    }
    return server;
  }

  protected ClusterNodeServerProvider nodeProvider() {
    return this.node().nodeServerProvider();
  }
}
