package de.dytanic.cloudnet.provider.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.UUID;

public class NodeSpecificCloudServiceProvider implements SpecificCloudServiceProvider {
    private CloudNet cloudNet;
    private ServiceInfoSnapshot serviceInfoSnapshot;

    public NodeSpecificCloudServiceProvider(CloudNet cloudNet, UUID uniqueId) {
        this(cloudNet, cloudNet.getCloudServiceProvider().getCloudService(uniqueId));
    }

    public NodeSpecificCloudServiceProvider(CloudNet cloudNet, String name) {
        this(cloudNet, cloudNet.getCloudServiceProvider().getCloudServiceByName(name));
    }

    public NodeSpecificCloudServiceProvider(CloudNet cloudNet, ServiceInfoSnapshot serviceInfoSnapshot) {
        this.cloudNet = cloudNet;
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }

    private ICloudService getCloudService() {
        if (this.serviceInfoSnapshot != null) {
            return this.cloudNet.getCloudServiceManager().getCloudService(this.serviceInfoSnapshot.getServiceId().getUniqueId());
        }
        throw new IllegalArgumentException("Cannot get CloudService of null");
    }

    @Override
    public ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync() {
        return this.cloudNet.scheduleTask(this::getServiceInfoSnapshot);
    }

    @Override
    public void addServiceTemplate(@NotNull ServiceTemplate serviceTemplate) {
        Validate.checkNotNull(serviceTemplate);

        ICloudService cloudService = this.getCloudService();

        if (cloudService != null) {
            cloudService.offerTemplate(serviceTemplate);
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
        if (serviceInfoSnapshot == null) {
            throw new IllegalStateException("Service does not exist");
        }

        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.addServiceTemplateToCloudService(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceTemplate);
        }
    }

    @Override
    public ITask<Void> addServiceTemplateAsync(@NotNull ServiceTemplate serviceTemplate) {
        return this.cloudNet.scheduleTask(() -> {
            this.addServiceTemplate(serviceTemplate);
            return null;
        });
    }

    @Override
    public void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
        Validate.checkNotNull(serviceRemoteInclusion);

        ICloudService cloudService = this.getCloudService();

        if (cloudService != null) {
            cloudService.offerInclusion(serviceRemoteInclusion);
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
        if (serviceInfoSnapshot == null) {
            throw new IllegalStateException("Service does not exist");
        }
        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.addServiceRemoteInclusionToCloudService(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceRemoteInclusion);
        }
    }

    @Override
    public ITask<Void> addServiceRemoteInclusionAsync(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
        return this.cloudNet.scheduleTask(() -> {
            this.addServiceRemoteInclusion(serviceRemoteInclusion);
            return null;
        });
    }

    @Override
    public void addServiceDeployment(@NotNull ServiceDeployment serviceDeployment) {
        Validate.checkNotNull(serviceDeployment);

        ICloudService cloudService = this.getCloudService();

        if (cloudService != null) {
            cloudService.addDeployment(serviceDeployment);
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
        if (serviceInfoSnapshot == null) {
            throw new IllegalStateException("Service does not exist");
        }
        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.addServiceDeploymentToCloudService(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceDeployment);
        }
    }

    @Override
    public ITask<Void> addServiceDeploymentAsync(@NotNull ServiceDeployment serviceDeployment) {
        return this.cloudNet.scheduleTask(() -> {
            this.addServiceDeployment(serviceDeployment);
            return null;
        });
    }

    @Override
    public Queue<String> getCachedLogMessages() {
        ICloudService cloudService = this.getCloudService();

        if (cloudService != null) {
            return cloudService.getServiceConsoleLogCache().getCachedLogMessages();
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
        if (serviceInfoSnapshot == null) {
            throw new IllegalStateException("Service does not exist");
        }
        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            return clusterNodeServer.getCachedLogMessagesFromService(serviceInfoSnapshot.getServiceId().getUniqueId());
        }
        return null;
    }

    @Override
    public ITask<Queue<String>> getCachedLogMessagesAsync() {
        return this.cloudNet.scheduleTask(this::getCachedLogMessages);
    }

    @Override
    public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
        Validate.checkNotNull(lifeCycle);

        ICloudService cloudService = this.getCloudService();
        if (cloudService != null) {
            switch (lifeCycle) {
                case RUNNING:
                    try {
                        cloudService.start();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    break;
                case STOPPED:
                    this.cloudNet.scheduleTask(() -> {
                        cloudService.stop();
                        return null;
                    });
                    break;
                case DELETED:
                    this.cloudNet.scheduleTask(() -> {
                        cloudService.delete();
                        return null;
                    });
                    break;
            }
        } else {
            ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
            if (serviceInfoSnapshot != null) {
                IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

                if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
                    clusterNodeServer.setCloudServiceLifeCycle(serviceInfoSnapshot, lifeCycle);
                }
            }
        }
    }

    @Override
    public ITask<Void> setCloudServiceLifeCycleAsync(@NotNull ServiceLifeCycle lifeCycle) {
        return this.cloudNet.scheduleTask(() -> {
            this.setCloudServiceLifeCycle(lifeCycle);
            return null;
        });
    }

    @Override
    public void restart() {
        ICloudService cloudService = this.getCloudService();
        if (cloudService != null) {
            try {
                cloudService.restart();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        } else {
            ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
            if (serviceInfoSnapshot != null) {
                IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

                if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
                    clusterNodeServer.restartCloudService(serviceInfoSnapshot);
                }
            }
        }
    }

    @Override
    public ITask<Void> restartAsync() {
        return this.cloudNet.scheduleTask(() -> {
            this.restart();
            return null;
        });
    }

    @Override
    public void kill() {
        ICloudService cloudService = this.getCloudService();
        if (cloudService != null) {
            try {
                cloudService.kill();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        } else {
            ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
            if (serviceInfoSnapshot != null) {
                IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

                if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
                    clusterNodeServer.killCloudService(serviceInfoSnapshot);
                }
            }
        }
    }

    @Override
    public ITask<Void> killAsync() {
        return this.cloudNet.scheduleTask(() -> {
            this.kill();
            return null;
        });
    }

    @Override
    public void runCommand(@NotNull String command) {
        ICloudService cloudService = this.getCloudService();

        if (cloudService != null) {
            cloudService.runCommand(command);
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
        if (serviceInfoSnapshot == null) {
            throw new IllegalStateException("Service does not exist");
        }

        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.runCommand(serviceInfoSnapshot, command);
        }
    }

    @Override
    public ITask<Void> runCommandAsync(@NotNull String command) {
        return this.cloudNet.scheduleTask(() -> {
            this.runCommand(command);
            return null;
        });
    }

    @Override
    public void includeWaitingServiceTemplates() {
        ICloudService cloudService = this.getCloudService();

        if (cloudService != null) {
            cloudService.includeTemplates();
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
        if (serviceInfoSnapshot == null) {
            throw new IllegalStateException("Service does not exist");
        }

        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.includeWaitingServiceTemplates(this.serviceInfoSnapshot.getServiceId().getUniqueId());
        }
    }

    @Override
    public void includeWaitingServiceInclusions() {
        ICloudService cloudService = this.getCloudService();

        if (cloudService != null) {
            cloudService.includeInclusions();
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
        if (serviceInfoSnapshot == null) {
            throw new IllegalStateException("Service does not exist");
        }

        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.includeWaitingServiceInclusions(serviceInfoSnapshot.getServiceId().getUniqueId());
        }
    }

    @Override
    public void deployResources(boolean removeDeployments) {
        ICloudService cloudService = this.getCloudService();

        if (cloudService != null) {
            cloudService.deployResources(removeDeployments);
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = this.getServiceInfoSnapshot();
        if (serviceInfoSnapshot == null) {
            throw new IllegalStateException("Service does not exist");
        }
        IClusterNodeServer clusterNodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(serviceInfoSnapshot.getServiceId().getNodeUniqueId());

        if (clusterNodeServer != null && clusterNodeServer.isConnected() && clusterNodeServer.getChannel() != null) {
            clusterNodeServer.deployResources(serviceInfoSnapshot.getServiceId().getUniqueId(), removeDeployments);
        }
    }

    @Override
    public ITask<Void> includeWaitingServiceTemplatesAsync() {
        return this.cloudNet.scheduleTask(() -> {
            this.includeWaitingServiceTemplates();
            return null;
        });
    }

    @Override
    public ITask<Void> includeWaitingServiceInclusionsAsync() {
        return this.cloudNet.scheduleTask(() -> {
            this.includeWaitingServiceInclusions();
            return null;
        });
    }

    @Override
    public ITask<Void> deployResourcesAsync(boolean removeDeployments) {
        return this.cloudNet.scheduleTask(() -> {
            this.deployResources(removeDeployments);
            return null;
        });
    }
}
