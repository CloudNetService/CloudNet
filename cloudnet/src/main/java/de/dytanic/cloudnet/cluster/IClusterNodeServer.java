package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Queue;
import java.util.UUID;

public interface IClusterNodeServer extends AutoCloseable {

    void sendClusterChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument header, byte[] body);

    void sendCustomChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

    @NotNull
    IClusterNodeServerProvider getProvider();

    @NotNull
    NetworkClusterNode getNodeInfo();

    void setNodeInfo(@NotNull NetworkClusterNode nodeInfo);

    NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot();

    void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot);

    INetworkChannel getChannel();

    void setChannel(@NotNull INetworkChannel channel);

    boolean isConnected();

    void saveSendPacket(@NotNull IPacket packet);

    boolean isAcceptableConnection(@NotNull INetworkChannel channel, @NotNull String nodeId);

    String[] sendCommandLine(@NotNull String commandLine);

    void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull byte[] zipResource);

    ServiceInfoSnapshot createCloudService(@NotNull ServiceTask serviceTask);

    ServiceInfoSnapshot createCloudService(@NotNull ServiceConfiguration serviceConfiguration);

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

    ServiceInfoSnapshot sendCommandLineToCloudService(@NotNull UUID uniqueId, @NotNull String commandLine);

    ServiceInfoSnapshot addServiceTemplateToCloudService(@NotNull UUID uniqueId, @NotNull ServiceTemplate serviceTemplate);

    ServiceInfoSnapshot addServiceRemoteInclusionToCloudService(@NotNull UUID uniqueId, @NotNull ServiceRemoteInclusion serviceRemoteInclusion);

    ServiceInfoSnapshot addServiceDeploymentToCloudService(@NotNull UUID uniqueId, @NotNull ServiceDeployment serviceDeployment);

    Queue<String> getCachedLogMessagesFromService(@NotNull UUID uniqueId);

    void setCloudServiceLifeCycle(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, @NotNull ServiceLifeCycle lifeCycle);

    void restartCloudService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

    void killCloudService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

    void runCommand(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, @NotNull String command);

    void includeWaitingServiceInclusions(@NotNull UUID uniqueId);

    void includeWaitingServiceTemplates(@NotNull UUID uniqueId);

    void deployResources(@NotNull UUID uniqueId, boolean removeDeployments);

    default void deployResources(@NotNull UUID uniqueId) {
        deployResources(uniqueId, true);
    }
}