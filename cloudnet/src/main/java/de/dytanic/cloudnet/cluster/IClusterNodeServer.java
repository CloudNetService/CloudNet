package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface IClusterNodeServer extends AutoCloseable {

    void sendCustomChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data);

    void sendCustomChannelMessage(@NotNull ChannelMessage channelMessage);

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

    @ApiStatus.Internal
    CloudServiceFactory getCloudServiceFactory();

    @ApiStatus.Internal
    SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

}