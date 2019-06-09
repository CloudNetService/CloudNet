package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

public final class V1HttpHandlerStatus extends V1HttpHandler {

    public V1HttpHandlerStatus(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) throws Exception {
        this.sendOptions(context, "OPTIONS, GET");
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        IConfiguration configuration = getCloudNet().getConfig();

        context
                .response()
                .header("Content-Type", "application/json")
                .body(
                        new JsonDocument()
                                .append("Version", V1HttpHandlerStatus.class.getPackage().getImplementationVersion())
                                .append("Version-Title", V1HttpHandlerStatus.class.getPackage().getImplementationTitle())
                                .append("Identity", configuration.getIdentity())
                                .append("currentNetworkClusterNodeInfoSnapshot", getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot())
                                .append("lastNetworkClusterNodeInfoSnapshot", getCloudNet().getLastNetworkClusterNodeInfoSnapshot())
                                .append("providedServicesCount", getCloudNet().getCloudServiceManager().getCloudServices().size())
                                .append("modules", Iterables.map(getCloudNet().getModuleProvider().getModules(), moduleWrapper -> moduleWrapper.getModuleConfiguration().getGroup() + ":" +
                                        moduleWrapper.getModuleConfiguration().getName() + ":" +
                                        moduleWrapper.getModuleConfiguration().getVersion()))
                                .append("clientConnections", Iterables.map(getCloudNet().getNetworkClient().getChannels(), channel -> channel.getServerAddress()))
                                .toByteArray()
                )
                .statusCode(200)
                .context()
                .closeAfter(true)
                .cancelNext()
        ;
    }
}