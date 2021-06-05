package de.dytanic.cloudnet;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public class EmptyCloudNetDriver extends CloudNetDriver {

    public EmptyCloudNetDriver() {
        super(null);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() {

    }

    @Override
    public @NotNull String getComponentName() {
        return null;
    }

    @Override
    public @NotNull String getNodeUniqueId() {
        return null;
    }

    @Override
    public @NotNull TemplateStorage getLocalTemplateStorage() {
        return null;
    }

    @Override
    public @Nullable TemplateStorage getTemplateStorage(String storage) {
        return null;
    }

    @Override
    public @NotNull Collection<TemplateStorage> getAvailableTemplateStorages() {
        return null;
    }

    @Override
    public @NotNull ITask<Collection<TemplateStorage>> getAvailableTemplateStoragesAsync() {
        return null;
    }

    @Override
    public @NotNull DatabaseProvider getDatabaseProvider() {
        return null;
    }

    @Override
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name) {
        return null;
    }

    @Override
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId) {
        return null;
    }

    @Override
    public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        return null;
    }

    @Override
    public @NotNull INetworkClient getNetworkClient() {
        return null;
    }

    @Override
    public @NotNull ITask<Collection<ServiceTemplate>> getLocalTemplateStorageTemplatesAsync() {
        return null;
    }

    @Override
    public @NotNull ITask<Collection<ServiceTemplate>> getTemplateStorageTemplatesAsync(@NotNull String serviceName) {
        return null;
    }

    @Override
    public Collection<ServiceTemplate> getLocalTemplateStorageTemplates() {
        return null;
    }

    @Override
    public Collection<ServiceTemplate> getTemplateStorageTemplates(@NotNull String serviceName) {
        return null;
    }

    @Override
    public void setGlobalLogLevel(@NotNull LogLevel logLevel) {

    }

    @Override
    public void setGlobalLogLevel(int logLevel) {

    }

    @Override
    public Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String commandLine) {
        return null;
    }

    @Override
    public @NotNull ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId, @NotNull String commandLine) {
        return null;
    }
}
