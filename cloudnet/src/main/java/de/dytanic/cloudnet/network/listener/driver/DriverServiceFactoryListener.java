package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public class DriverServiceFactoryListener extends CategorizedDriverAPIListener {
    public DriverServiceFactoryListener() {
        super(DriverAPICategory.CLOUD_SERVICE_FACTORY);

        super.registerHandler(DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_CONFIGURATION, (channel, packet, input) -> {
            ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(input.readObject(ServiceConfiguration.class));
            return ProtocolBuffer.create().writeOptionalObject(snapshot);
        });

    }
}
