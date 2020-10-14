package de.dytanic.cloudnet.wrapper.provider;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class WrapperGroupConfigurationProvider implements GroupConfigurationProvider, DriverAPIUser {

    private final Wrapper wrapper;

    public WrapperGroupConfigurationProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public void reload() {
        this.reloadAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public Collection<GroupConfiguration> getGroupConfigurations() {
        return this.getGroupConfigurationsAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public void setGroupConfigurations(@NotNull Collection<GroupConfiguration> groupConfigurations) {
        this.setGroupConfigurationsAsync(groupConfigurations).get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public GroupConfiguration getGroupConfiguration(@NotNull String name) {
        Preconditions.checkNotNull(name);
        return this.getGroupConfigurationAsync(name).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public boolean isGroupConfigurationPresent(@NotNull String name) {
        Preconditions.checkNotNull(name);
        return this.isGroupConfigurationPresentAsync(name).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public void addGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);
        this.addGroupConfigurationAsync(groupConfiguration).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public void removeGroupConfiguration(@NotNull String name) {
        Preconditions.checkNotNull(name);
        this.removeGroupConfigurationAsync(name).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public void removeGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);
        this.removeGroupConfiguration(groupConfiguration.getName());
    }

    @Override
    public @NotNull ITask<Void> reloadAsync() {
        return this.executeVoidDriverAPIMethod(DriverAPIRequestType.RELOAD_GROUPS, null);
    }

    @Override
    @NotNull
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_GROUP_CONFIGURATIONS,
                packet -> packet.getBuffer().readObjectCollection(GroupConfiguration.class)
        );
    }

    @Override
    public @NotNull ITask<Void> setGroupConfigurationsAsync(@NotNull Collection<GroupConfiguration> groupConfigurations) {
        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.SET_GROUP_CONFIGURATIONS,
                buffer -> buffer.writeObjectCollection(groupConfigurations)
        );
    }

    @Override
    @NotNull
    public ITask<GroupConfiguration> getGroupConfigurationAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_GROUP_CONFIGURATION_BY_NAME,
                buffer -> buffer.writeString(name),
                packet -> packet.getBuffer().readOptionalObject(GroupConfiguration.class)
        );
    }

    @Override
    @NotNull
    public ITask<Boolean> isGroupConfigurationPresentAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.IS_GROUP_CONFIGURATION_PRESENT,
                buffer -> buffer.writeString(name),
                packet -> packet.getBuffer().readBoolean()
        );
    }

    @Override
    @NotNull
    public ITask<Void> addGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);

        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.ADD_GROUP_CONFIGURATION,
                buffer -> buffer.writeObject(groupConfiguration)
        );
    }

    @Override
    @NotNull
    public ITask<Void> removeGroupConfigurationAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.executeVoidDriverAPIMethod(
                DriverAPIRequestType.REMOVE_GROUP_CONFIGURATION,
                buffer -> buffer.writeString(name)
        );
    }

    @Override
    @NotNull
    public ITask<Void> removeGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration) {
        Preconditions.checkNotNull(groupConfiguration);

        return this.removeGroupConfigurationAsync(groupConfiguration.getName());
    }

    @Override
    public INetworkChannel getNetworkChannel() {
        return this.wrapper.getNetworkChannel();
    }
}
