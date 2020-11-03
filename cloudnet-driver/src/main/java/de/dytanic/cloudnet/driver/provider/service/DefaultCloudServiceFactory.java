package de.dytanic.cloudnet.driver.provider.service;

import de.dytanic.cloudnet.common.concurrent.CountingTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public abstract class DefaultCloudServiceFactory implements CloudServiceFactory {

    @Override
    public @Nullable ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        return this.createCloudService(ServiceConfiguration.builder(serviceTask).build());
    }

    @Override
    public @Nullable ServiceInfoSnapshot createCloudService(ServiceTask serviceTask, int taskId) {
        return this.createCloudService(ServiceConfiguration.builder(serviceTask).taskId(taskId).build());
    }

    @Override
    public @Nullable ServiceInfoSnapshot createCloudService(
            String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
            Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        return this.createCloudService(ServiceConfiguration.builder()
                .task(name)
                .runtime(runtime)
                .autoDeleteOnStop(autoDeleteOnStop)
                .staticService(staticService)
                .inclusions(includes)
                .templates(templates)
                .deployments(deployments)
                .groups(groups)
                .maxHeapMemory(processConfiguration.getMaxHeapMemorySize())
                .jvmOptions(processConfiguration.getJvmOptions())
                .processParameters(processConfiguration.getProcessParameters())
                .environment(processConfiguration.getEnvironment())
                .properties(properties)
                .startPort(port == null ? 44955 : port)
                .build());
    }

    @Override
    public @Nullable Collection<ServiceInfoSnapshot> createCloudService(
            String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
            Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Collection<ServiceInfoSnapshot> snapshots = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            ServiceInfoSnapshot snapshot = this.createCloudService(ServiceConfiguration.builder()
                    .node(nodeUniqueId)
                    .task(name)
                    .runtime(runtime)
                    .autoDeleteOnStop(autoDeleteOnStop)
                    .staticService(staticService)
                    .inclusions(includes)
                    .templates(templates)
                    .deployments(deployments)
                    .groups(groups)
                    .maxHeapMemory(processConfiguration.getMaxHeapMemorySize())
                    .jvmOptions(processConfiguration.getJvmOptions())
                    .processParameters(processConfiguration.getProcessParameters())
                    .environment(processConfiguration.getEnvironment())
                    .properties(properties)
                    .startPort(port == null ? processConfiguration.getEnvironment().getDefaultStartPort() : port)
                    .build());
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return snapshots;
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        return this.createCloudServiceAsync(ServiceConfiguration.builder(serviceTask).build());
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask, int taskId) {
        return this.createCloudServiceAsync(ServiceConfiguration.builder(serviceTask).taskId(taskId).build());
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(
            String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
            Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        return this.createCloudServiceAsync(ServiceConfiguration.builder()
                .task(name)
                .runtime(runtime)
                .autoDeleteOnStop(autoDeleteOnStop)
                .staticService(staticService)
                .inclusions(includes)
                .templates(templates)
                .deployments(deployments)
                .groups(groups)
                .maxHeapMemory(processConfiguration.getMaxHeapMemorySize())
                .jvmOptions(processConfiguration.getJvmOptions())
                .environment(processConfiguration.getEnvironment())
                .properties(properties)
                .startPort(port == null ? 44955 : port)
                .build());
    }

    @Override
    public @NotNull ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(
            String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes, Collection<ServiceTemplate> templates, Collection<ServiceDeployment> deployments,
            Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Collection<ServiceInfoSnapshot> snapshots = new ArrayList<>(amount);
        CountingTask<Collection<ServiceInfoSnapshot>> task = new CountingTask<>(snapshots, amount);

        for (int i = 0; i < amount; i++) {
            this.createCloudServiceAsync(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port)
                    .onComplete(serviceInfoSnapshot -> {
                        snapshots.add(serviceInfoSnapshot);
                        task.countDown();
                    }).onFailure(throwable -> task.countDown()).onCancelled(e -> task.countDown());
        }

        return task;
    }

}
