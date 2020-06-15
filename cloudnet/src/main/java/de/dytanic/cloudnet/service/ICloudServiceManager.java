package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public interface ICloudServiceManager {

    @NotNull
    File getTempDirectory();

    @NotNull
    File getPersistenceServicesDirectory();

    Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots();

    Map<UUID, ICloudService> getCloudServices();

    Map<String, ICloudServiceFactory> getCloudServiceFactories();

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

    int getCurrentUsedHeapMemory();

    int getCurrentReservedMemory();

}