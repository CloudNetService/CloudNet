package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;

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
                if (path.endsWith("/command")) {
                    // a command should be executed on the node
                    this.handleNodeCommandRequest(context);
                } else {
                    // specific node was requested
                    this.handleNodeRequest(context);
                }
            } else {
                // a list of all nodes was requested
                this.handleNodeListRequest(context);
            }
        } else if (context.request().method().equalsIgnoreCase("POST")) {
            // post is used for creation of nodes
            this.handleNodeCreateRequest(context);
        } else if (context.request().method().equalsIgnoreCase("DELETE")) {
            // delete a cluster server from the configuration
            this.handleNodeDeleteRequest(context);
        } else if (context.request().method().equalsIgnoreCase("PUT")) {
            // put (modifies) an existing cluster node
            this.handleNodeUpdateRequest(context);
        }
    }

    protected void handleNodeRequest(IHttpContext context) {
        NodeServer server = this.getNodeServer(context, true);
        if (server != null) {
            this.ok(context)
                    .body(this.success().append("node", this.createNodeInfoDocument(server)).toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        } else {
            this.response(context, HttpResponseCode.HTTP_NOT_FOUND)
                    .body(this.failure().append("reason", "No such node found").toByteArray())
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
                .body(this.success().append("nodes", nodes).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleNodeCommandRequest(IHttpContext context) {
        NodeServer nodeServer = this.getNodeServer(context, true);
        String commandLine = this.body(context.request()).getString("command");
        if (commandLine == null || nodeServer == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", nodeServer == null ? "Unknown node server" : "Missing command line").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        String[] result = nodeServer.sendCommandLine(commandLine);
        this.ok(context)
                .body(this.success().append("result", result).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleNodeCreateRequest(IHttpContext context) {
        NetworkClusterNode server = this.body(context.request()).toInstanceOf(NetworkClusterNode.class);
        if (server == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing node server information").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        if (this.getNodeServer(server.getUniqueId(), true) != null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "The node server is already registered").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        IConfiguration configuration = this.getConfiguration();
        configuration.getClusterConfig().getNodes().add(server);
        configuration.save();

        this.getNodeProvider().setClusterServers(configuration.getClusterConfig());

        this.response(context, HttpResponseCode.HTTP_CREATED)
                .body(this.success().toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleNodeDeleteRequest(IHttpContext context) {
        String uniqueId = context.request().pathParameters().get("node");
        if (uniqueId == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "No node unique id provided").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        boolean removed = this.getConfiguration().getClusterConfig().getNodes().removeIf(
                node -> node.getUniqueId().equals(uniqueId));
        if (removed) {
            this.getConfiguration().save();
            this.getNodeProvider().setClusterServers(this.getConfiguration().getClusterConfig());

            this.response(context, HttpResponseCode.HTTP_OK)
                    .body(this.success().toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        } else {
            this.response(context, HttpResponseCode.HTTP_NOT_FOUND)
                    .body(this.failure().append("reason", "No node with that unique id present").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        }
    }

    protected void handleNodeUpdateRequest(IHttpContext context) {
        NetworkClusterNode server = this.body(context.request()).toInstanceOf(NetworkClusterNode.class);
        if (server == null) {
            this.badRequest(context)
                    .body(this.failure().append("reason", "Missing node server information").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
            return;
        }

        NetworkClusterNode registered = this.getConfiguration().getClusterConfig().getNodes()
                .stream()
                .filter(node -> node.getUniqueId().equals(server.getUniqueId()))
                .findFirst()
                .orElse(null);
        if (registered == null) {
            this.response(context, HttpResponseCode.HTTP_NOT_FOUND)
                    .body(this.failure().append("reason", "No node with that unique id present").toByteArray())
                    .context()
                    .closeAfter(true)
                    .cancelNext();
        } else {
            registered.setListeners(server.getListeners());
            registered.getProperties().append(server.getProperties().toJsonObject());
            this.getConfiguration().save();
            this.getNodeProvider().setClusterServers(this.getConfiguration().getClusterConfig());

            this.ok(context)
                    .body(this.success().toByteArray())
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
        String nodeName = context.request().pathParameters().get("node");
        return nodeName == null ? null : this.getNodeServer(nodeName, includeLocal);
    }

    protected NodeServer getNodeServer(String nodeName, boolean includeLocal) {
        NodeServer server = this.getNodeProvider().getNodeServer(nodeName);
        if (server == null && includeLocal && nodeName.equals(CloudNet.getInstance().getComponentName())) {
            server = this.getNodeProvider().getSelfNode();
        }
        return server;
    }

    protected IClusterNodeServerProvider getNodeProvider() {
        return this.getCloudNet().getClusterNodeServerProvider();
    }
}
