package de.dytanic.cloudnet.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class NodeGeneralCloudServiceProvider implements GeneralCloudServiceProvider {

    private final CloudNet cloudNet;

    public NodeGeneralCloudServiceProvider(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @Override
    public Collection<UUID> getServicesAsUniqueId() {
        return Collections.unmodifiableCollection(this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().keySet());
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot getCloudServiceByName(String name) {
        return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values().stream()
                .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices() {
        return this.cloudNet.getCloudServiceManager().getServiceInfoSnapshots();
    }

    @Override
    public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
        return this.getCloudServices().stream().filter(serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING).collect(Collectors.toList());
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices(String taskName) {
        Preconditions.checkNotNull(taskName);

        return this.cloudNet.getCloudServiceManager().getServiceInfoSnapshots(taskName);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices(ServiceEnvironmentType environment) {
        Preconditions.checkNotNull(environment);

        return this.cloudNet.getCloudServiceManager().getServiceInfoSnapshots(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getEnvironment() == environment);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServicesByGroup(String group) {
        Preconditions.checkNotNull(group);

        return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()
                .stream()
                .filter(serviceInfoSnapshot -> Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(group))
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot getCloudService(UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.cloudNet.getCloudServiceManager().getServiceInfoSnapshot(uniqueId);
    }

    @Override
    public int getServicesCount() {
        return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().size();
    }

    @Override
    public int getServicesCountByGroup(String group) {
        Preconditions.checkNotNull(group);

        int amount = 0;

        for (ServiceInfoSnapshot serviceInfoSnapshot : this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()) {
            if (Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(group)) {
                amount++;
            }
        }

        return amount;
    }

    @Override
    public int getServicesCountByTask(String taskName) {
        Preconditions.checkNotNull(taskName);

        int amount = 0;

        for (ServiceInfoSnapshot serviceInfoSnapshot : this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()) {
            if (serviceInfoSnapshot.getServiceId().getTaskName().equals(taskName)) {
                amount++;
            }
        }

        return amount;
    }

    @Override
    @NotNull
    public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
        return this.cloudNet.scheduleTask(this::getServicesAsUniqueId);
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name) {
        return this.cloudNet.scheduleTask(() -> this.getCloudServiceByName(name));
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
        return this.cloudNet.scheduleTask(this::getCloudServices);
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync() {
        return this.cloudNet.scheduleTask(this::getStartedCloudServices);
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName) {
        Preconditions.checkNotNull(taskName);

        return this.cloudNet.scheduleTask(() -> this.getCloudServices(taskName));
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(ServiceEnvironmentType environment) {
        return this.cloudNet.scheduleTask(() -> this.getCloudServices(environment));
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group) {
        Preconditions.checkNotNull(group);

        return this.cloudNet.scheduleTask(() -> this.getCloudServicesByGroup(group));
    }

    @Override
    @NotNull
    public ITask<Integer> getServicesCountAsync() {
        return this.cloudNet.scheduleTask(this::getServicesCount);
    }

    @Override
    @NotNull
    public ITask<Integer> getServicesCountByGroupAsync(String group) {
        Preconditions.checkNotNull(group);

        return this.cloudNet.scheduleTask(() -> this.getServicesCountByGroup(group));
    }

    @Override
    @NotNull
    public ITask<Integer> getServicesCountByTaskAsync(String taskName) {
        Preconditions.checkNotNull(taskName);

        return this.cloudNet.scheduleTask(() -> this.getServicesCountByTask(taskName));
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> getCloudServiceAsync(UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.cloudNet.scheduleTask(() -> this.getCloudService(uniqueId));
    }
}
