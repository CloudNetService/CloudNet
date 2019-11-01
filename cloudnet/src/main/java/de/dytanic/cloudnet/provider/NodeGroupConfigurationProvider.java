package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;

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

    @Override
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return this.cloudNet.scheduleTask(this::getGroupConfigurations);
    }

    @Override
    public ITask<GroupConfiguration> getGroupConfigurationAsync(String name) {
        return this.cloudNet.scheduleTask(() -> this.getGroupConfiguration(name));
    }

    @Override
    public ITask<Boolean> isGroupConfigurationPresentAsync(String name) {
        return this.cloudNet.scheduleTask(() -> this.isGroupConfigurationPresent(name));
    }

    @Override
    public ITask<Void> addGroupConfigurationAsync(GroupConfiguration groupConfiguration) {
        return this.cloudNet.scheduleTask(() -> {
            this.addGroupConfiguration(groupConfiguration);
            return null;
        });
    }

    @Override
    public ITask<Void> removeGroupConfigurationAsync(String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.removeGroupConfiguration(name);
            return null;
        });
    }

    @Override
    public ITask<Void> removeGroupConfigurationAsync(GroupConfiguration groupConfiguration) {
        return this.cloudNet.scheduleTask(() -> {
            this.removeGroupConfiguration(groupConfiguration);
            return null;
        });
    }

}
