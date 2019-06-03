package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.util.function.Function;
import java.util.function.Predicate;

public final class V1HttpHandlerCluster extends V1HttpHandler {

    public V1HttpHandlerCluster(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) throws Exception {
        this.sendOptions(context, "OPTIONS, GET");
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        if (context.request().pathParameters().containsKey("node"))
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(
                            Iterables.map(
                                    Iterables.filter(getCloudNet().getClusterNodeServerProvider().getNodeServers(), new Predicate<IClusterNodeServer>() {
                                        @Override
                                        public boolean test(IClusterNodeServer iClusterNodeServer) {
                                            return iClusterNodeServer.getNodeInfo().getUniqueId().toLowerCase().contains(context.request().pathParameters().get("node"));
                                        }
                                    }), new Function<IClusterNodeServer, JsonDocument>() {
                                        @Override
                                        public JsonDocument apply(IClusterNodeServer iClusterNodeServer) {
                                            return new JsonDocument()
                                                    .append("node", iClusterNodeServer.getNodeInfo())
                                                    .append("nodeInfoSnapshot", iClusterNodeServer.getNodeInfoSnapshot())
                                                    ;
                                        }
                                    })))
                    .context()
                    .closeAfter(true)
                    .cancelNext()
                    ;
        else
            context
                    .response()
                    .statusCode(HttpResponseCode.HTTP_OK)
                    .header("Content-Type", "application/json")
                    .body(GSON.toJson(
                            Iterables.map(
                                    Iterables.filter(getCloudNet().getClusterNodeServerProvider().getNodeServers(), new Predicate<IClusterNodeServer>() {
                                        @Override
                                        public boolean test(IClusterNodeServer iClusterNodeServer) {
                                            return !context.request().queryParameters().containsKey("uniqueId") ||
                                                    containsStringElementInCollection(context.request().queryParameters().get("uniqueId"),
                                                            iClusterNodeServer.getNodeInfo().getUniqueId());
                                        }
                                    }), new Function<IClusterNodeServer, JsonDocument>() {
                                        @Override
                                        public JsonDocument apply(IClusterNodeServer iClusterNodeServer) {
                                            return new JsonDocument()
                                                    .append("node", iClusterNodeServer.getNodeInfo())
                                                    .append("nodeInfoSnapshot", iClusterNodeServer.getNodeInfoSnapshot())
                                                    ;
                                        }
                                    })))
                    .context()
                    .closeAfter(true)
                    .cancelNext()
                    ;
    }
}