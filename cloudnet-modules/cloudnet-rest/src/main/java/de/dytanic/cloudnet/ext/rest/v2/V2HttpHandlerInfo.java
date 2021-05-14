package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;

import java.util.stream.Collectors;

public class V2HttpHandlerInfo extends V2HttpHandler {

    public V2HttpHandlerInfo() {
        super(null, "GET");
    }

    @Override
    protected void handleUnauthorized(String path, IHttpContext context) {
        this.response(context, HttpResponseCode.HTTP_NO_CONTENT).context().closeAfter(true).cancelNext();
    }

    @Override
    protected void handleBasicAuthorized(String path, IHttpContext context, IPermissionUser user) {
        this.sendNodeInformation(context);
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
        this.sendNodeInformation(context);
    }

    protected void sendNodeInformation(IHttpContext context) {
        JsonDocument information = this.success()
                .append("title", CloudNet.class.getPackage().getImplementationTitle())
                .append("version", CloudNet.class.getPackage().getImplementationVersion())
                .append("nodeInfoSnapshot", this.getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot())
                .append("lastNodeInfoSnapshot", this.getCloudNet().getLastNetworkClusterNodeInfoSnapshot())
                .append("serviceCount", this.getCloudNet().getCloudServiceProvider().getServicesCount())
                .append("clientConnections", super.getCloudNet().getNetworkClient().getChannels().stream()
                        .map(INetworkChannel::getServerAddress)
                        .collect(Collectors.toList()));
        this.ok(context).body(information.toByteArray()).context().closeAfter(true).cancelNext();
    }
}
