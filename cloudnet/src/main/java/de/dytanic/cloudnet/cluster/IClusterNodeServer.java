package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Collection;
import java.util.Queue;
import java.util.UUID;

public interface IClusterNodeServer extends AutoCloseable {

    void sendClusterChannelMessage(String channel, String message, JsonDocument header, byte[] body);

    void sendCustomChannelMessage(String channel, String message, JsonDocument data);

    IClusterNodeServerProvider getProvider();

    NetworkClusterNode getNodeInfo();

    void setNodeInfo(NetworkClusterNode nodeInfo);

    NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot();

    void setNodeInfoSnapshot(NetworkClusterNodeInfoSnapshot nodeInfoSnapshot);

    INetworkChannel getChannel();

    void setChannel(INetworkChannel channel);

    boolean isConnected();

    void saveSendPacket(IPacket packet);

    boolean isAcceptableConnection(INetworkChannel channel, String nodeId);

    String[] sendCommandLine(String commandLine);

    void deployTemplateInCluster(ServiceTemplate serviceTemplate, byte[] zipResource);

    ServiceInfoSnapshot createCloudService(ServiceTask serviceTask);

    ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration);

    ServiceInfoSnapshot createCloudService(String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
                                           Collection<ServiceRemoteInclusion> includes,
                                           Collection<ServiceTemplate> templates,
                                           Collection<ServiceDeployment> deployments,
                                           Collection<String> groups,
                                           ProcessConfiguration processConfiguration,
                                           JsonDocument properties, Integer port);

    Collection<ServiceInfoSnapshot> createCloudService(
            String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties, Integer port);

    default ServiceInfoSnapshot createCloudService(String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
                                                   Collection<ServiceRemoteInclusion> includes,
                                                   Collection<ServiceTemplate> templates,
                                                   Collection<ServiceDeployment> deployments,
                                                   Collection<String> groups,
                                                   ProcessConfiguration processConfiguration, Integer port) {
        return createCloudService(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    default Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
                                                               Collection<ServiceRemoteInclusion> includes,
                                                               Collection<ServiceTemplate> templates,
                                                               Collection<ServiceDeployment> deployments,
                                                               Collection<String> groups,
                                                               ProcessConfiguration processConfiguration, Integer port) {
        return createCloudService(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, JsonDocument.newDocument(), port);
    }

    ServiceInfoSnapshot sendCommandLineToCloudService(UUID uniqueId, String commandLine);

    ServiceInfoSnapshot addServiceTemplateToCloudService(UUID uniqueId, ServiceTemplate serviceTemplate);

    ServiceInfoSnapshot addServiceRemoteInclusionToCloudService(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion);

    ServiceInfoSnapshot addServiceDeploymentToCloudService(UUID uniqueId, ServiceDeployment serviceDeployment);

    Queue<String> getCachedLogMessagesFromService(UUID uniqueId);

    void setCloudServiceLifeCycle(ServiceInfoSnapshot serviceInfoSnapshot, ServiceLifeCycle lifeCycle);

    void restartCloudService(ServiceInfoSnapshot serviceInfoSnapshot);

    void killCloudService(ServiceInfoSnapshot serviceInfoSnapshot);

    void runCommand(ServiceInfoSnapshot serviceInfoSnapshot, String command);

    void includeWaitingServiceInclusions(UUID uniqueId);

    void includeWaitingServiceTemplates(UUID uniqueId);

    void deployResources(UUID uniqueId, boolean removeDeployments);

    default void deployResources(UUID uniqueId) {
        deployResources(uniqueId, true);
    }
}