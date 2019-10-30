package de.dytanic.cloudnet.service.provider;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.provider.GroupConfigurationProvider;

import java.util.Collection;

public class NodeGroupConfigurationProvider implements GroupConfigurationProvider {

    private CloudNet cloudNet;

    public NodeGroupConfigurationProvider(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @Override
    public Collection<GroupConfiguration> getGroupConfigurations() {
        return this.cloudNet.getCloudServiceManager().getGroupConfigurations();
    }

    @Override
    public GroupConfiguration getGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        return this.cloudNet.getCloudServiceManager().getGroupConfiguration(name);
    }

    @Override
    public boolean isGroupConfigurationPresent(String name) {
        Validate.checkNotNull(name);

        return this.cloudNet.getCloudServiceManager().isGroupConfigurationPresent(name);
    }

    @Override
    public void addGroupConfiguration(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);

        this.cloudNet.getCloudServiceManager().addGroupConfiguration(groupConfiguration);
    }

    @Override
    public void removeGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        this.cloudNet.getCloudServiceManager().removeGroupConfiguration(name);
    }

    @Override
    public void removeGroupConfiguration(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);

        this.cloudNet.getCloudServiceManager().removeGroupConfiguration(groupConfiguration);
    }
    
}
