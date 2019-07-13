package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public interface ICloudServiceManager {

    File getTempDirectory();

    File getPersistenceServicesDirectory();

    Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots();

    Map<UUID, ICloudService> getCloudServices();

    Map<String, ICloudServiceFactory> getCloudServiceFactories();

    /*= -------------------------------------------------- =*/

    List<ServiceTask> getServiceTasks();

    void setServiceTasks(Collection<ServiceTask> tasks);

    void addPermanentServiceTask(ServiceTask task);

    void removePermanentServiceTask(ServiceTask task);

    void removePermanentServiceTask(String name);

    void removeAllPermanentServiceTasks();

    ServiceTask getServiceTask(String name);

    boolean isTaskPresent(String name);

    //-

    List<GroupConfiguration> getGroupConfigurations();

    void setGroupConfigurations(Collection<GroupConfiguration> groupConfigurations);

    GroupConfiguration getGroupConfiguration(String name);

    void addGroupConfiguration(GroupConfiguration groupConfiguration);

    void removeGroupConfiguration(GroupConfiguration groupConfiguration);

    void removeGroupConfiguration(String name);

    boolean isGroupConfigurationPresent(String group);

    //-

    ICloudService runTask(ServiceTask serviceTask);

    ICloudService runTask(ServiceConfiguration serviceConfiguration);

    ICloudService runTask(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    );

    default ICloudService runTask(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            Integer port
    ){
        return runTask(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, port);
    }

    void startAllCloudServices();

    void stopAllCloudServices();

    void deleteAllCloudServices();

    //-

    ICloudService getCloudService(UUID uniqueId);

    ICloudService getCloudService(Predicate<ICloudService> predicate);

    Collection<ICloudService> getCloudServices(String taskName);

    Collection<ICloudService> getCloudServices(Predicate<ICloudService> predicate);

    Collection<ICloudService> getServices();

    ServiceInfoSnapshot getServiceInfoSnapshot(UUID uniqueId);

    ServiceInfoSnapshot getServiceInfoSnapshot(Predicate<ServiceInfoSnapshot> predicate);

    Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(String taskName);

    Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(ServiceEnvironmentType environment);

    Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(Predicate<ServiceInfoSnapshot> predicate);

    Collection<ServiceInfoSnapshot> getServiceInfoSnapshots();

    Collection<Integer> getReservedTaskIds(String task);

    //-

    void reload();

    int getCurrentUsedHeapMemory();

    int getCurrentReservedMemory();

}