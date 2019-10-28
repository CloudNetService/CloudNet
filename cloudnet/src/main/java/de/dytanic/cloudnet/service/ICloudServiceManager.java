package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.*;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

public interface ICloudServiceManager {

    File getTempDirectory();

    File getPersistenceServicesDirectory();

    Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots();

    Map<UUID, ICloudService> getCloudServices();

    Map<String, ICloudServiceFactory> getCloudServiceFactories();

    void init();


    List<ServiceTask> getServiceTasks();

    void setServiceTasks(Collection<ServiceTask> tasks);

    void setServiceTasksWithoutClusterSync(Collection<ServiceTask> tasks);

    default void updatePermanentServiceTask(ServiceTask task) {
        this.addPermanentServiceTask(task);
    }

    boolean addPermanentServiceTask(ServiceTask task);

    void removePermanentServiceTask(ServiceTask task);

    void removePermanentServiceTask(String name);

    boolean addPermanentServiceTaskWithoutClusterSync(ServiceTask task);

    void removePermanentServiceTaskWithoutClusterSync(ServiceTask task);

    void removePermanentServiceTaskWithoutClusterSync(String name);

    void removeAllPermanentServiceTasks();

    ServiceTask getServiceTask(String name);

    boolean isTaskPresent(String name);

    //-

    default void updateGroupConfiguration(GroupConfiguration groupConfiguration) {
        this.addGroupConfiguration(groupConfiguration);
    }

    List<GroupConfiguration> getGroupConfigurations();

    void setGroupConfigurations(Collection<GroupConfiguration> groupConfigurations);

    void setGroupConfigurationsWithoutClusterSync(Collection<GroupConfiguration> groupConfigurations);

    GroupConfiguration getGroupConfiguration(String name);

    void addGroupConfiguration(GroupConfiguration groupConfiguration);

    void removeGroupConfiguration(GroupConfiguration groupConfiguration);

    void addGroupConfigurationWithoutClusterSync(GroupConfiguration groupConfiguration);

    void removeGroupConfigurationWithoutClusterSync(GroupConfiguration groupConfiguration);

    void removeGroupConfiguration(String name);

    void removeGroupConfigurationWithoutClusterSync(String name);

    boolean isGroupConfigurationPresent(String group);

    //-

    ICloudService runTask(ServiceTask serviceTask);

    ICloudService runTask(ServiceConfiguration serviceConfiguration);

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
            JsonDocument properties,
            Integer port
    ) {
        return this.runTask(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, Collections.emptyList(), processConfiguration, properties, port);
    }

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
    ) {
        return runTask(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    ICloudService runTask(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            Collection<String> deletedFilesAfterStop,
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
            Collection<String> deletedFilesAfterStop,
            ProcessConfiguration processConfiguration,
            Integer port
    ) {
        return runTask(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, deletedFilesAfterStop, processConfiguration, JsonDocument.newDocument(), port);
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