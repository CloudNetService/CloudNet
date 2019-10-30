package de.dytanic.cloudnet.driver.service.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.util.Collection;
import java.util.UUID;

public interface GeneralCloudServiceProvider {

    Collection<UUID> getServicesAsUniqueId();

    ServiceInfoSnapshot getCloudServiceByName(String name);

    Collection<ServiceInfoSnapshot> getCloudServices();

    Collection<ServiceInfoSnapshot> getStartedCloudServices();

    Collection<ServiceInfoSnapshot> getCloudServices(String taskName);

    Collection<ServiceInfoSnapshot> getCloudServicesByGroup(String group);

    ServiceInfoSnapshot getCloudService(UUID uniqueId);

    int getServicesCount();

    int getServicesCountByGroup(String group);

    int getServicesCountByTask(String taskName);

    ITask<Collection<UUID>> getServicesAsUniqueIdAsync();

    ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(String name);

    ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync();

    ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync();

    ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(String taskName);

    ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(String group);

    ITask<Integer> getServicesCountAsync();

    ITask<Integer> getServicesCountByGroupAsync(String group);

    ITask<Integer> getServicesCountByTaskAsync(String taskName);

    ITask<ServiceInfoSnapshot> getCloudServiceAsync(UUID uniqueId);

}
