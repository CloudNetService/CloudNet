package de.dytanic.cloudnet.wrapper.provider;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class WrapperGroupConfigurationProvider implements GroupConfigurationProvider {

    private static final Function<Pair<JsonDocument, byte[]>, Void> VOID_FUNCTION = documentPair -> null;

    private Wrapper wrapper;

    public WrapperGroupConfigurationProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Collection<GroupConfiguration> getGroupConfigurations() {
        try {
            return this.getGroupConfigurationsAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public GroupConfiguration getGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        try {
            return this.getGroupConfigurationAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isGroupConfigurationPresent(String name) {
        Validate.checkNotNull(name);

        try {
            return this.isGroupConfigurationPresentAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void addGroupConfiguration(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);

        try {
            this.addGroupConfigurationAsync(groupConfiguration).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeGroupConfiguration(String name) {
        Validate.checkNotNull(name);

        try {
            this.removeGroupConfigurationAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeGroupConfiguration(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);
        this.removeGroupConfiguration(groupConfiguration.getName());
    }

    @Override
    public ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync() {
        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_groupConfigurations"), null,
                documentPair -> documentPair.getFirst().get("groupConfigurations", new TypeToken<Collection<GroupConfiguration>>() {
                }.getType()));
    }

    @Override
    public ITask<GroupConfiguration> getGroupConfigurationAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_group_configuration").append("name", name), null,
                documentPair -> documentPair.getFirst().get("groupConfiguration", new TypeToken<GroupConfiguration>() {
                }.getType()));
    }

    @Override
    public ITask<Boolean> isGroupConfigurationPresentAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "is_group_configuration_present").append("name", name), null,
                documentPair -> documentPair.getFirst().get("result", new TypeToken<Boolean>() {
                }.getType()));
    }

    @Override
    public ITask<Void> addGroupConfigurationAsync(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_group_configuration").append("groupConfiguration", groupConfiguration), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> removeGroupConfigurationAsync(String name) {
        Validate.checkNotNull(name);

        return this.wrapper.sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "remove_group_configuration").append("name", name), null,
                VOID_FUNCTION);
    }

    @Override
    public ITask<Void> removeGroupConfigurationAsync(GroupConfiguration groupConfiguration) {
        Validate.checkNotNull(groupConfiguration);

        return this.removeGroupConfigurationAsync(groupConfiguration.getName());
    }
}
