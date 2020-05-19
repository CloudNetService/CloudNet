package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

public interface ICloudServiceManager {

    @NotNull
    File getTempDirectory();

    @NotNull
    File getPersistenceServicesDirectory();

    Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots();

    Map<UUID, ICloudService> getCloudServices();

    Map<String, ICloudServiceFactory> getCloudServiceFactories();

    void init();


    List<ServiceTask> getServiceTasks();

    void setServiceTasks(@NotNull Collection<ServiceTask> tasks);

    void setServiceTasksWithoutClusterSync(@NotNull Collection<ServiceTask> tasks);

    default void updatePermanentServiceTask(@NotNull ServiceTask task) {
        this.addPermanentServiceTask(task);
    }

    boolean addPermanentServiceTask(@NotNull ServiceTask task);

    void removePermanentServiceTask(@NotNull ServiceTask task);

    void removePermanentServiceTask(@NotNull String name);

    boolean addPermanentServiceTaskWithoutClusterSync(@NotNull ServiceTask task);

    void removePermanentServiceTaskWithoutClusterSync(@NotNull ServiceTask task);

    void removePermanentServiceTaskWithoutClusterSync(@NotNull String name);

    void removeAllPermanentServiceTasks();

    @Nullable
    ServiceTask getServiceTask(@NotNull String name);

    boolean isTaskPresent(@NotNull String name);

    //-

    default void updateGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
        this.addGroupConfiguration(groupConfiguration);
    }

    List<GroupConfiguration> getGroupConfigurations();

    void setGroupConfigurations(@NotNull Collection<GroupConfiguration> groupConfigurations);

    void setGroupConfigurationsWithoutClusterSync(@NotNull Collection<GroupConfiguration> groupConfigurations);

    GroupConfiguration getGroupConfiguration(@NotNull String name);

    void addGroupConfiguration(@NotNull GroupConfiguration groupConfiguration);

    void removeGroupConfiguration(@NotNull GroupConfiguration groupConfiguration);

    void addGroupConfigurationWithoutClusterSync(@NotNull GroupConfiguration groupConfiguration);

    void removeGroupConfigurationWithoutClusterSync(@NotNull GroupConfiguration groupConfiguration);

    void removeGroupConfiguration(@NotNull String name);

    void removeGroupConfigurationWithoutClusterSync(@NotNull String name);

    boolean isGroupConfigurationPresent(@NotNull String group);

    //-

    ICloudService runTask(@NotNull ServiceTask serviceTask);

    ICloudService runTask(@NotNull ServiceTask serviceTask, int taskId);

    ICloudService runTask(@NotNull ServiceConfiguration serviceConfiguration);

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
        return this.runTask(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
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

    ICloudService runTask(
            String name,
            String runtime,
            int taskId,
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
        return this.runTask(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, deletedFilesAfterStop, processConfiguration, JsonDocument.newDocument(), port);
    }

    void startAllCloudServices();

    void stopAllCloudServices();

    void deleteAllCloudServices();

    //-

    @Nullable
    ICloudService getCloudService(@NotNull UUID uniqueId);

    @Nullable
    ICloudService getCloudService(@NotNull Predicate<ICloudService> predicate);

    /**
     * @deprecated moved to {@link #getLocalCloudServices(String)}
     */
    @Deprecated
    default Collection<ICloudService> getCloudServices(String taskName) {
        return this.getLocalCloudServices(taskName);
    }

    /**
     * @deprecated moved to {@link #getLocalCloudServices(Predicate)}
     */
    @Deprecated
    default Collection<ICloudService> getCloudServices(Predicate<ICloudService> predicate) {
        return this.getLocalCloudServices(predicate);
    }

    /**
     * @deprecated moved to {@link #getLocalCloudServices()}
     */
    @Deprecated
    default Collection<ICloudService> getServices() {
        return this.getLocalCloudServices();
    }

    Collection<ICloudService> getLocalCloudServices(@NotNull String taskName);

    Collection<ICloudService> getLocalCloudServices(@NotNull Predicate<ICloudService> predicate);

    Collection<ICloudService> getLocalCloudServices();

    @Nullable
    ServiceInfoSnapshot getServiceInfoSnapshot(@NotNull UUID uniqueId);

    @Nullable
    ServiceInfoSnapshot getServiceInfoSnapshot(@NotNull Predicate<ServiceInfoSnapshot> predicate);

    Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(@NotNull String taskName);

    Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(@NotNull ServiceEnvironmentType environment);

    Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(@NotNull Predicate<ServiceInfoSnapshot> predicate);

    Collection<ServiceInfoSnapshot> getServiceInfoSnapshots();

    Collection<Integer> getReservedTaskIds(@NotNull String task);

    //-

    void reload();

    int getCurrentUsedHeapMemory();

    int getCurrentReservedMemory();

    boolean isFileCreated();

}