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

package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.V2HttpHandler;
import java.util.Collection;
import java.util.stream.Collectors;

public class V2HttpHandlerCluster extends V2HttpHandler {

  public V2HttpHandlerCluster(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE", "PUT");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
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

  protected void handleNodeRequest(IHttpContext context) {
    var server = this.getNodeServer(context, true);
    if (server != null) {
      this.ok(context)
        .body(this.success().append("node", this.createNodeInfoDocument(server)).toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.response(context, HttpResponseCode.HTTP_NOT_FOUND)
        .body(this.failure().append("reason", "No such node found").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected void handleNodeListRequest(IHttpContext context) {
    Collection<JsonDocument> nodes = this.getNodeProvider().getNodeServers().stream()
      .map(this::createNodeInfoDocument)
      .collect(Collectors.toList());
    // add the local node info
    nodes.add(this.createNodeInfoDocument(this.getNodeProvider().getSelfNode()));

    this.ok(context)
      .body(this.success().append("nodes", nodes).toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleNodeCommandRequest(IHttpContext context) {
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

  protected void handleNodeCreateRequest(IHttpContext context) {
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

    var configuration = this.getConfiguration();
    configuration.getClusterConfig().nodes().add(server);
    configuration.save();

    this.getNodeProvider().setClusterServers(configuration.getClusterConfig());

    this.response(context, HttpResponseCode.HTTP_CREATED)
      .body(this.success().toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected void handleNodeDeleteRequest(IHttpContext context) {
    var uniqueId = context.request().pathParameters().get("node");
    if (uniqueId == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "No node unique id provided").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var removed = this.getConfiguration().getClusterConfig().nodes().removeIf(
      node -> node.uniqueId().equals(uniqueId));
    if (removed) {
      this.getConfiguration().save();
      this.getNodeProvider().setClusterServers(this.getConfiguration().getClusterConfig());

      this.response(context, HttpResponseCode.HTTP_OK)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      this.response(context, HttpResponseCode.HTTP_NOT_FOUND)
        .body(this.failure().append("reason", "No node with that unique id present").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected void handleNodeUpdateRequest(IHttpContext context) {
    var server = this.body(context.request()).toInstanceOf(NetworkClusterNode.class);
    if (server == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing node server information").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var registered = this.getConfiguration().getClusterConfig().nodes()
      .stream()
      .filter(node -> node.uniqueId().equals(server.uniqueId()))
      .findFirst()
      .orElse(null);
    if (registered == null) {
      this.response(context, HttpResponseCode.HTTP_NOT_FOUND)
        .body(this.failure().append("reason", "No node with that unique id present").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    } else {
      //TODO: registered.setListeners(server.getListeners());
      registered.properties().append(server.properties());
      this.getConfiguration().save();
      this.getNodeProvider().setClusterServers(this.getConfiguration().getClusterConfig());

      this.ok(context)
        .body(this.success().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    }
  }

  protected JsonDocument createNodeInfoDocument(NodeServer node) {
    return JsonDocument.newDocument("node", node.getNodeInfo())
      .append("available", node.isAvailable())
      .append("head", node.isHeadNode())
      .append("nodeInfoSnapshot", node.getNodeInfoSnapshot());
  }

  protected NodeServer getNodeServer(IHttpContext context, boolean includeLocal) {
    var nodeName = context.request().pathParameters().get("node");
    return nodeName == null ? null : this.getNodeServer(nodeName, includeLocal);
  }

  protected NodeServer getNodeServer(String nodeName, boolean includeLocal) {
    NodeServer server = this.getNodeProvider().getNodeServer(nodeName);
    if (server == null && includeLocal && nodeName.equals(CloudNet.getInstance().componentName())) {
      server = this.getNodeProvider().getSelfNode();
    }
    return server;
  }

  protected IClusterNodeServerProvider getNodeProvider() {
    return this.getCloudNet().getClusterNodeServerProvider();
  }
}
