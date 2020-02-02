package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.CloudNet;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    @Override
    public GroupConfiguration getGroupConfiguration(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.cloudNet.getCloudServiceManager().getGroupConfiguration(name);
    }

    @Override
    public boolean isGroupConfigurationPresent(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.cloudNet.getCloudServiceManager().isGroupConfigurationPresent(name);
    }

    @Override
    public void addGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);

        this.cloudNet.getCloudServiceManager().addGroupConfiguration(groupConfiguration);
    }

    @Override
    public void removeGroupConfiguration(@NotNull String name) {
        Preconditions.checkNotNull(name);

        this.cloudNet.getCloudServiceManager().removeGroupConfiguration(name);
    }

    @Override
    public void removeGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);

        this.cloudNet.getCloudServiceManager().removeGroupConfiguration(groupConfiguration);
    }

    @Override
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return this.cloudNet.scheduleTask(this::getGroupConfigurations);
    }

    @Override
    public ITask<GroupConfiguration> getGroupConfigurationAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> this.getGroupConfiguration(name));
    }

    @Override
    public ITask<Boolean> isGroupConfigurationPresentAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> this.isGroupConfigurationPresent(name));
    }

    @Override
    public ITask<Void> addGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration) {
        return this.cloudNet.scheduleTask(() -> {
            this.addGroupConfiguration(groupConfiguration);
            return null;
        });
    }

    @Override
    public ITask<Void> removeGroupConfigurationAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.removeGroupConfiguration(name);
            return null;
        });
    }

    @Override
    public ITask<Void> removeGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration) {
        return this.cloudNet.scheduleTask(() -> {
            this.removeGroupConfiguration(groupConfiguration);
            return null;
        });
    }

}
