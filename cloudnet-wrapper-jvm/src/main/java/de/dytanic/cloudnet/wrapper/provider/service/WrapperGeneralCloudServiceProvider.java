package de.dytanic.cloudnet.wrapper.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.DriverAPIUser;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WrapperGeneralCloudServiceProvider implements GeneralCloudServiceProvider, DriverAPIUser {

    private final Wrapper wrapper;

    public WrapperGeneralCloudServiceProvider(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Collection<UUID> getServicesAsUniqueId() {
        return this.getServicesAsUniqueIdAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot getCloudServiceByName(String name) {
        return this.getCloudServiceByNameAsync(name).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices() {
        return this.getCloudServicesAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
        return this.getStartedCloudServicesAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices(String taskName) {
        return this.getCloudServicesAsync(taskName).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices(ServiceEnvironmentType environment) {
        Preconditions.checkNotNull(environment);
        return this.getCloudServicesAsync(environment).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServicesByGroup(String group) {
        return this.getCloudServicesByGroupAsync(group).get(5, TimeUnit.SECONDS, null);
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot getCloudService(UUID uniqueId) {
        return this.getCloudServiceAsync(uniqueId).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public int getServicesCount() {
        return this.getServicesCountAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public int getServicesCountByGroup(String group) {
        return this.getServicesCountByGroupAsync(group).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public int getServicesCountByTask(String taskName) {
        return this.getServicesCountByTaskAsync(taskName).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    @NotNull
    public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_SERVICES_AS_UNIQUE_ID,
                packet -> packet.getBuffer().readUUIDCollection()
        );
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_CLOUD_SERVICE_BY_NAME,
                buffer -> buffer.writeString(name),
                packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_CLOUD_SERVICES,
                packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_STARTED_CLOUD_SERVICES,
                packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_CLOUD_SERVICES_BY_SERVICE_TASK,
                buffer -> buffer.writeString(taskName),
                packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(ServiceEnvironmentType environment) {
        Preconditions.checkNotNull(environment);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_CLOUD_SERVICES_BY_ENVIRONMENT,
                buffer -> buffer.writeEnumConstant(environment),
                packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_CLOUD_SERVICES_BY_GROUP,
                buffer -> buffer.writeString(group),
                packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
        );
    }

    @Override
    @NotNull
    public ITask<Integer> getServicesCountAsync() {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_SERVICES_COUNT,
                packet -> packet.getBuffer().readInt()
        );
    }

    @Override
    @NotNull
    public ITask<Integer> getServicesCountByGroupAsync(String group) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_SERVICES_COUNT_BY_GROUP,
                buffer -> buffer.writeString(group),
                packet -> packet.getBuffer().readInt()
        );
    }

    @Override
    @NotNull
    public ITask<Integer> getServicesCountByTaskAsync(String taskName) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_SERVICES_COUNT_BY_TASK,
                buffer -> buffer.writeString(taskName),
                packet -> packet.getBuffer().readInt()
        );
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> getCloudServiceAsync(UUID uniqueId) {
        return this.executeDriverAPIMethod(
                DriverAPIRequestType.GET_CLOUD_SERVICE_BY_UNIQUE_ID,
                buffer -> buffer.writeUUID(uniqueId),
                packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class)
        );
    }

    @Override
    public INetworkClient getNetworkClient() {
        return this.wrapper.getNetworkClient();
    }
}
