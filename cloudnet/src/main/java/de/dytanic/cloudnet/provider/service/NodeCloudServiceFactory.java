package de.dytanic.cloudnet.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.DefaultCloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class NodeCloudServiceFactory extends DefaultCloudServiceFactory implements CloudServiceFactory {

    private final CloudNet cloudNet;

    public NodeCloudServiceFactory(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        Preconditions.checkNotNull(serviceConfiguration);

        if (serviceConfiguration.getServiceId() == null) {
            return null;
        }

        String node = serviceConfiguration.getServiceId().getNodeUniqueId();
        if (node == null) {
            Collection<String> allowedNodes = serviceConfiguration.getServiceId().getAllowedNodes();
            if (allowedNodes == null) {
                allowedNodes = Collections.emptyList();
            }
            NetworkClusterNodeInfoSnapshot snapshot = this.cloudNet.searchLogicNode(allowedNodes);
            if (snapshot == null) {
                return null;
            }

            node = snapshot.getNode().getUniqueId();
        }

        if (this.cloudNet.getConfig().getIdentity().getUniqueId().equals(node)) {
            ICloudService cloudService = this.cloudNet.getCloudServiceManager().runTask(serviceConfiguration);
            return cloudService != null ? cloudService.getServiceInfoSnapshot() : null;
        } else {
            IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider().getNodeServer(node);

            if (server != null && server.isConnected()) {
                return server.getCloudServiceFactory().createCloudService(serviceConfiguration);
            }
        }

        return null;
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        return this.cloudNet.scheduleTask(() -> this.createCloudService(serviceConfiguration));
    }

}
