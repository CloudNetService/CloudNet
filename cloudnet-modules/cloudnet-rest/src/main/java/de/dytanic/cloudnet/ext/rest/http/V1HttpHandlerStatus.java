package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.util.stream.Collectors;

public final class V1HttpHandlerStatus extends V1HttpHandler {

    public V1HttpHandlerStatus(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) {
        this.sendOptions(context, "OPTIONS, GET");
    }

    @Override
    public void handleGet(String path, IHttpContext context) {
        IConfiguration configuration = this.getCloudNet().getConfig();

        context
                .response()
                .header("Content-Type", "application/json")
                .body(
                        new JsonDocument()
                                .append("Version", V1HttpHandlerStatus.class.getPackage().getImplementationVersion())
                                .append("Version-Title", V1HttpHandlerStatus.class.getPackage().getImplementationTitle())
                                .append("Identity", configuration.getIdentity())
                                .append("currentNetworkClusterNodeInfoSnapshot", this.getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot())
                                .append("lastNetworkClusterNodeInfoSnapshot", this.getCloudNet().getLastNetworkClusterNodeInfoSnapshot())
                                .append("providedServicesCount", this.getCloudNet().getCloudServiceManager().getCloudServices().size())
                                .append("modules", super.getCloudNet().getModuleProvider().getModules().stream()
                                        .map(moduleWrapper -> moduleWrapper.getModuleConfiguration().getGroup() + ":" +
                                                moduleWrapper.getModuleConfiguration().getName() + ":" +
                                                moduleWrapper.getModuleConfiguration().getVersion())
                                        .collect(Collectors.toList()))
                                .append("clientConnections", super.getCloudNet().getNetworkClient().getChannels().stream()
                                        .map(INetworkChannel::getServerAddress)
                                        .collect(Collectors.toList()))
                                .toByteArray()
                )
                .statusCode(200)
                .context()
                .closeAfter(true)
                .cancelNext()
        ;
    }
}