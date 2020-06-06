package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;

import java.util.Collection;

public class DriverGroupListener extends CategorizedDriverAPIListener {
    public DriverGroupListener() {
        super(DriverAPICategory.GROUP_CONFIGURATIONS);

        super.registerHandler(DriverAPIRequestType.ADD_GROUP_CONFIGURATION, (channel, packet, input) -> {
            CloudNetDriver.getInstance().getGroupConfigurationProvider().addGroupConfiguration(input.readObject(GroupConfiguration.class));
            return ProtocolBuffer.EMPTY;
        });

        super.registerHandler(DriverAPIRequestType.REMOVE_GROUP_CONFIGURATION, (channel, packet, input) -> {
            CloudNetDriver.getInstance().getGroupConfigurationProvider().removeGroupConfiguration(input.readString());
            return ProtocolBuffer.EMPTY;
        });

        super.registerHandler(DriverAPIRequestType.GET_GROUP_CONFIGURATIONS, (channel, packet, input) -> {
            Collection<GroupConfiguration> groups = CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfigurations();
            return ProtocolBuffer.create().writeObjectCollection(groups);
        });

        super.registerHandler(DriverAPIRequestType.GET_GROUP_CONFIGURATION_BY_NAME, (channel, packet, input) -> {
            GroupConfiguration group = CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfiguration(input.readString());
            return ProtocolBuffer.create().writeOptionalObject(group);
        });

        super.registerHandler(DriverAPIRequestType.IS_GROUP_CONFIGURATION_PRESENT, (channel, packet, input) -> {
            boolean present = CloudNetDriver.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(input.readString());
            return ProtocolBuffer.create().writeBoolean(present);
        });

    }
}
