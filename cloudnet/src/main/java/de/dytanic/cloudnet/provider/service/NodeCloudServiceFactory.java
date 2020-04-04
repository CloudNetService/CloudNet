package de.dytanic.cloudnet.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class NodeCloudServiceFactory implements CloudServiceFactory {

    private CloudNet cloudNet;

    public NodeCloudServiceFactory(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        return this.executeOnLogicNode(serviceTask,
                cloudServiceManager -> cloudServiceManager.runTask(serviceTask),
                clusterNodeServer -> clusterNodeServer.createCloudService(serviceTask)
        );
    }

    @Override
    @Nullable
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask, int taskId) {
        return this.executeOnLogicNode(serviceTask,
                cloudServiceManager -> cloudServiceManager.runTask(serviceTask, taskId),
                clusterNodeServer -> clusterNodeServer.createCloudService(serviceTask, taskId)
        );
    }

    private ServiceInfoSnapshot executeOnLogicNode(ServiceTask serviceTask, Function<ICloudServiceManager, ICloudService> localHandler,
                                                   Function<IClusterNodeServer, ServiceInfoSnapshot> remoteHandler) {
        Preconditions.checkNotNull(serviceTask);

        try {
            NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot = this.cloudNet.searchLogicNode(serviceTask);
            if (networkClusterNodeInfoSnapshot == null) {
                return null;
            }

            if (this.cloudNet.getConfig().getIdentity().getUniqueId().equals(networkClusterNodeInfoSnapshot.getNode().getUniqueId())) {
                ICloudService cloudService = localHandler.apply(this.cloudNet.getCloudServiceManager());
                return cloudService != null ? cloudService.getServiceInfoSnapshot() : null;
            } else {
                IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(networkClusterNodeInfoSnapshot.getNode().getUniqueId());

                if (clusterNodeServer != null && clusterNodeServer.isConnected()) {
                    return remoteHandler.apply(clusterNodeServer);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        Preconditions.checkNotNull(serviceConfiguration);

        if (serviceConfiguration.getServiceId() == null || serviceConfiguration.getServiceId().getNodeUniqueId() == null) {
            return null;
        }

        if (this.cloudNet.getConfig().getIdentity().getUniqueId().equals(serviceConfiguration.getServiceId().getNodeUniqueId())) {
            ICloudService cloudService = this.cloudNet.getCloudServiceManager().runTask(serviceConfiguration);
            return cloudService != null ? cloudService.getServiceInfoSnapshot() : null;
        } else {
            IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceConfiguration.getServiceId().getNodeUniqueId());

            if (clusterNodeServer != null && clusterNodeServer.isConnected()) {
                return clusterNodeServer.createCloudService(serviceConfiguration);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(String name, String runtime, boolean autoDeleteOnStop, boolean staticService, Collection<ServiceRemoteInclusion> includes,
                                                  Collection<ServiceTemplate> templates,
                                                  Collection<ServiceDeployment> deployments,
                                                  Collection<String> groups,
                                                  ProcessConfiguration processConfiguration,
                                                  JsonDocument properties, Integer port) {
        ICloudService cloudService = this.cloudNet.getCloudServiceManager().runTask(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port);
        return cloudService != null ? cloudService.getServiceInfoSnapshot() : null;
    }

    @Nullable
    @Override
    public Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups,
                                                              ProcessConfiguration processConfiguration,
                                                              JsonDocument properties, Integer port) {
        Preconditions.checkNotNull(nodeUniqueId);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        if (this.cloudNet.getConfig().getIdentity().getUniqueId().equals(nodeUniqueId)) {
            Collection<ServiceInfoSnapshot> collection = new ArrayList<>();

            for (int i = 0; i < amount; i++) {
                ICloudService cloudService = this.cloudNet.getCloudServiceManager().runTask(
                        name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port != null ? port++ : null
                );

                if (cloudService != null) {
                    collection.add(cloudService.getServiceInfoSnapshot());
                }
            }

            return collection;
        }

        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(nodeUniqueId);

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port);
        } else {
            return null;
        }
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        return this.cloudNet.scheduleTask(() -> this.createCloudService(serviceTask));
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask, int taskId) {
        return this.cloudNet.scheduleTask(() -> this.createCloudService(serviceTask, taskId));
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        return this.cloudNet.scheduleTask(() -> this.createCloudService(serviceConfiguration));
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        return this.cloudNet.scheduleTask(() -> this.createCloudService(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port));
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(
            String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups, ProcessConfiguration processConfiguration, JsonDocument properties, Integer port) {
        Preconditions.checkNotNull(nodeUniqueId);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        return this.cloudNet.scheduleTask(() -> this.createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port));
    }
}
