package de.dytanic.cloudnet.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.DefaultCloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.network.packet.PacketServerStartServiceFromConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NodeCloudServiceFactory extends DefaultCloudServiceFactory implements CloudServiceFactory {

    private final CloudNet cloudNet;

    public NodeCloudServiceFactory(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @Override
    public @Nullable ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        try {
            return this.createCloudServiceAsync(serviceConfiguration).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration configuration) {
        if (this.cloudNet.isHeadNode()) {
            return this.createCloudServiceAsHeadNode(configuration);
        } else {
            return this.cloudNet.getClusterNodeServerProvider().getHeadNode().instance().getCloudServiceFactory().createCloudServiceAsync(configuration);
        }
    }

    @NotNull
    private ITask<ServiceInfoSnapshot> createCloudServiceAsHeadNode(ServiceConfiguration serviceConfiguration) {
        Preconditions.checkNotNull(serviceConfiguration);

        String node = serviceConfiguration.getServiceId().getNodeUniqueId();
        if (node == null) {
            Collection<String> allowedNodes = serviceConfiguration.getServiceId().getAllowedNodes();
            if (allowedNodes == null) {
                allowedNodes = Collections.emptyList();
            }

            NetworkClusterNodeInfoSnapshot snapshot = this.cloudNet.searchLogicNode(allowedNodes);
            if (snapshot == null) {
                return CompletedTask.create(null);
            }

            node = snapshot.getNode().getUniqueId();
            serviceConfiguration.getServiceId().setNodeUniqueId(node);
        }

        return this.cloudNet.getCloudServiceManager().buildService(serviceConfiguration).map(info -> {
            if (info == null || info.getServiceId().getNodeUniqueId().equals(this.cloudNet.getComponentName())) {
                return info;
            } else {
                IClusterNodeServer nodeServer = this.cloudNet.getClusterNodeServerProvider().getNodeServer(info.getServiceId().getNodeUniqueId());
                if (nodeServer == null || !nodeServer.isConnected() || nodeServer.getNodeInfoSnapshot() == null) {
                    this.cloudNet.getCloudServiceManager().removeService(info.getServiceId().getUniqueId());
                    return null;
                }

                nodeServer.getNodeInfoSnapshot().addReservedMemory(serviceConfiguration.getProcessConfig().getMaxHeapMemorySize());
                ServiceInfoSnapshot result = nodeServer.getNetworkChannel().sendQueryAsync(new PacketServerStartServiceFromConfiguration(serviceConfiguration)).map(response -> {
                    if (response == null) {
                        this.cloudNet.getCloudServiceManager().removeService(info.getServiceId().getUniqueId());
                        nodeServer.getNodeInfoSnapshot().removeReservedMemory(serviceConfiguration.getProcessConfig().getMaxHeapMemorySize());
                        return null;
                    }

                    return response.getBuffer().readOptionalObject(ServiceInfoSnapshot.class);
                }).get(30, TimeUnit.SECONDS, null);
                if (result == null) {
                    this.cloudNet.getCloudServiceManager().removeService(info.getServiceId().getUniqueId());
                    nodeServer.getNodeInfoSnapshot().removeReservedMemory(serviceConfiguration.getProcessConfig().getMaxHeapMemorySize());
                }

                return result;
            }
        });
    }
}
