package de.dytanic.cloudnet.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.DefaultCloudServiceFactory;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.util.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NodeCloudServiceFactory extends DefaultCloudServiceFactory implements CloudServiceFactory {

    private static final String HEAD_NODE_INFO_CHANNEL = "head_node_info";
    private static final String START_SERVICE_MESSAGE = "start_service";

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
                if (nodeServer == null) {
                    this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().remove(info.getServiceId().getUniqueId());
                    return null;
                }

                nodeServer.getNodeInfoSnapshot().addReservedMemory(serviceConfiguration.getProcessConfig().getMaxHeapMemorySize());
                ServiceInfoSnapshot result = nodeServer.sendChannelMessageQuery(ChannelMessage.builder()
                        .channel(HEAD_NODE_INFO_CHANNEL)
                        .message(START_SERVICE_MESSAGE)
                        .buffer(ProtocolBuffer.create().writeObject(info.getConfiguration()))
                        .targetNode(nodeServer.getNodeInfo().getUniqueId())
                        .build()
                ).map(response -> {
                    if (response == null) {
                        this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().remove(info.getServiceId().getUniqueId());
                        nodeServer.getNodeInfoSnapshot().removeReservedMemory(serviceConfiguration.getProcessConfig().getMaxHeapMemorySize());
                        return null;
                    }

                    return response.getBuffer().readOptionalObject(ServiceInfoSnapshot.class);
                }).get(30, TimeUnit.SECONDS, null);
                if (result == null) {
                    this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().remove(info.getServiceId().getUniqueId());
                    nodeServer.getNodeInfoSnapshot().removeReservedMemory(serviceConfiguration.getProcessConfig().getMaxHeapMemorySize());
                }

                return result;
            }
        });
    }

    @EventListener
    public void handleChannelMessage(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(HEAD_NODE_INFO_CHANNEL) || event.getMessage() == null
                || !event.getMessage().equals(START_SERVICE_MESSAGE) || event.getSender().getName() == null) {
            return;
        }

        Identity<NodeServer> headNode = this.cloudNet.getClusterNodeServerProvider().getHeadNode();
        if (headNode.instance().getNodeInfo().getUniqueId().equals(event.getSender().getName())) {
            ServiceInfoSnapshot snapshot = this.cloudNet.getCloudServiceManager()
                    .buildService(event.getBuffer().readObject(ServiceConfiguration.class))
                    .get(10, TimeUnit.SECONDS, null);
            event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                    .buffer(ProtocolBuffer.create().writeOptionalObject(snapshot))
                    .build());
        } else {
            event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                    .buffer(ProtocolBuffer.create().writeOptionalObject(this.createCloudService(event.getBuffer().readObject(ServiceConfiguration.class))))
                    .build());
        }
    }
}
