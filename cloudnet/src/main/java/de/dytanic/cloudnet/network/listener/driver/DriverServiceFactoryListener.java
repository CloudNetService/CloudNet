package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Collection;
import java.util.Collections;

public class DriverServiceFactoryListener extends CategorizedDriverAPIListener {
    public DriverServiceFactoryListener() {
        super(DriverAPICategory.CLOUD_SERVICE_FACTORY);

        super.registerHandler(DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_SERVICE_TASK, (channel, packet, input) -> {
            ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(input.readObject(ServiceTask.class));
            return ProtocolBuffer.create().writeOptionalObject(snapshot);
        });

        super.registerHandler(DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_SERVICE_TASK_AND_ID, (channel, packet, input) -> {
            ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(input.readObject(ServiceTask.class), input.readInt());
            return ProtocolBuffer.create().writeOptionalObject(snapshot);
        });

        super.registerHandler(DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_CONFIGURATION, (channel, packet, input) -> {
            ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(input.readObject(ServiceConfiguration.class));
            return ProtocolBuffer.create().writeOptionalObject(snapshot);
        });

        super.registerHandler(DriverAPIRequestType.CREATE_CUSTOM_CLOUD_SERVICE, (channel, packet, input) -> {
            String name = input.readString();
            String runtime = input.readString();
            boolean autoDeleteOnStop = input.readBoolean();
            boolean staticService = input.readBoolean();
            Collection<ServiceRemoteInclusion> includes = input.readObjectCollection(ServiceRemoteInclusion.class);
            Collection<ServiceTemplate> templates = input.readObjectCollection(ServiceTemplate.class);
            Collection<ServiceDeployment> deployments = input.readObjectCollection(ServiceDeployment.class);
            Collection<String> groups = input.readStringCollection();
            ProcessConfiguration processConfiguration = input.readObject(ProcessConfiguration.class);
            JsonDocument properties = input.readJsonDocument();
            int port = input.readInt();

            ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(
                    name, runtime,
                    autoDeleteOnStop, staticService,
                    includes, templates, deployments,
                    groups, processConfiguration,
                    properties, port
            );

            return ProtocolBuffer.create().writeOptionalObject(snapshot);
        });

        super.registerHandler(DriverAPIRequestType.CREATE_CUSTOM_CLOUD_SERVICE_WITH_NODE_AND_AMOUNT, (channel, packet, input) -> {
            String nodeUniqueId = input.readString();
            int amount = input.readInt();
            String name = input.readString();
            String runtime = input.readString();
            boolean autoDeleteOnStop = input.readBoolean();
            boolean staticService = input.readBoolean();
            Collection<ServiceRemoteInclusion> includes = input.readObjectCollection(ServiceRemoteInclusion.class);
            Collection<ServiceTemplate> templates = input.readObjectCollection(ServiceTemplate.class);
            Collection<ServiceDeployment> deployments = input.readObjectCollection(ServiceDeployment.class);
            Collection<String> groups = input.readStringCollection();
            ProcessConfiguration processConfiguration = input.readObject(ProcessConfiguration.class);
            JsonDocument properties = input.readJsonDocument();
            int port = input.readInt();

            Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(
                    nodeUniqueId, amount,
                    name, runtime,
                    autoDeleteOnStop, staticService,
                    includes, templates, deployments,
                    groups, processConfiguration,
                    properties, port
            );

            return ProtocolBuffer.create().writeObjectCollection(snapshots != null ? snapshots : Collections.emptyList());
        });

    }
}
