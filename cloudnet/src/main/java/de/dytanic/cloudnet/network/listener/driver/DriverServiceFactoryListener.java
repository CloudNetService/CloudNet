package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.concurrent.TimeUnit;

public class DriverServiceFactoryListener extends CategorizedDriverAPIListener {
    public DriverServiceFactoryListener() {
        super(DriverAPICategory.CLOUD_SERVICE_FACTORY);

        super.registerHandler(DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_CONFIGURATION, (channel, packet, input) -> {
            ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(input.readObject(ServiceConfiguration.class));
            return ProtocolBuffer.create().writeOptionalObject(snapshot);
        });

        super.registerHandler(DriverAPIRequestType.FORCE_CREATE_CLOUD_SERVICE_BY_CONFIGURATION, (channel, packet, input) -> {
            NodeServer server = CloudNet.getInstance().getClusterNodeServerProvider().getNodeServer(channel);
            if (server == null || !server.isHeadNode()) {
                // this message is only accepted by a head node
                return ProtocolBuffer.create().writeBoolean(false);
            } else {
                ServiceConfiguration configuration = input.readObject(ServiceConfiguration.class);
                long timeoutMillis = packet.getCreationMillis() + 5000;

                ICloudService service = CloudNet.getInstance().getCloudServiceManager()
                        .createCloudService(configuration, timeoutMillis)
                        .get(5, TimeUnit.SECONDS, null);
                return ProtocolBuffer.create().writeOptionalObject(service == null ? null : service.getServiceInfoSnapshot());
            }
        });
    }
}
