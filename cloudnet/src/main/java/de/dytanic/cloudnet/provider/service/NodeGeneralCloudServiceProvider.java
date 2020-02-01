package de.dytanic.cloudnet.provider.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class NodeGeneralCloudServiceProvider implements GeneralCloudServiceProvider {

    private CloudNet cloudNet;

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
        Validate.checkNotNull(taskName);

        return this.cloudNet.getCloudServiceManager().getServiceInfoSnapshots(taskName);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServices(ServiceEnvironmentType environment) {
        Validate.checkNotNull(environment);

        return this.cloudNet.getCloudServiceManager().getServiceInfoSnapshots(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getEnvironment() == environment);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getCloudServicesByGroup(String group) {
        Validate.checkNotNull(group);

        return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()
                .stream()
                .filter(serviceInfoSnapshot -> Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(group))
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot getCloudService(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.cloudNet.getCloudServiceManager().getServiceInfoSnapshot(uniqueId);
    }

    @Override
    public int getServicesCount() {
        return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().size();
    }

    @Override
    public int getServicesCountByGroup(String group) {
        Validate.checkNotNull(group);

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
        Validate.checkNotNull(taskName);

        int amount = 0;

        for (ServiceInfoSnapshot serviceInfoSnapshot : this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()) {
            if (serviceInfoSnapshot.getServiceId().getTaskName().equals(taskName)) {
                amount++;
            }
        }

        return amount;
    }

    @Override
    public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
        return this.cloudNet.scheduleTask(this::getServicesAsUniqueId);
    }

    @Override
    public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name) {
        return this.cloudNet.scheduleTask(() -> this.getCloudServiceByName(name));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
        return this.cloudNet.scheduleTask(this::getCloudServices);
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync() {
        return this.cloudNet.scheduleTask(this::getStartedCloudServices);
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName) {
        Validate.checkNotNull(taskName);

        return this.cloudNet.scheduleTask(() -> this.getCloudServices(taskName));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(ServiceEnvironmentType environment) {
        return this.cloudNet.scheduleTask(() -> this.getCloudServices(environment));
    }

    @Override
    public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return this.cloudNet.scheduleTask(() -> this.getCloudServicesByGroup(group));
    }

    @Override
    public ITask<Integer> getServicesCountAsync() {
        return this.cloudNet.scheduleTask(this::getServicesCount);
    }

    @Override
    public ITask<Integer> getServicesCountByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return this.cloudNet.scheduleTask(() -> this.getServicesCountByGroup(group));
    }

    @Override
    public ITask<Integer> getServicesCountByTaskAsync(String taskName) {
        Validate.checkNotNull(taskName);

        return this.cloudNet.scheduleTask(() -> this.getServicesCountByTask(taskName));
    }

    @Override
    public ITask<ServiceInfoSnapshot> getCloudServiceAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.cloudNet.scheduleTask(() -> this.getCloudService(uniqueId));
    }
}
