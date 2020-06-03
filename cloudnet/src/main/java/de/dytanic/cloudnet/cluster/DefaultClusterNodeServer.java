package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.RemoteSpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.network.packet.PacketServerDeployLocalTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class DefaultClusterNodeServer implements IClusterNodeServer, DriverAPIUser {

    private final DefaultClusterNodeServerProvider provider;

    private final CloudServiceFactory cloudServiceFactory = new ClusterNodeCloudServiceFactory(this::getChannel, this);

    private volatile NetworkClusterNodeInfoSnapshot nodeInfoSnapshot;

    private NetworkClusterNode nodeInfo;

    private INetworkChannel channel;

    DefaultClusterNodeServer(DefaultClusterNodeServerProvider provider, NetworkClusterNode nodeInfo) {
        this.provider = provider;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public void sendCustomChannelMessage(@NotNull ChannelMessage channelMessage) {
        this.saveSendPacket(new PacketClientServerChannelMessage(channelMessage, false));
    }

    @Override
    public void sendCustomChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data) {
        this.sendCustomChannelMessage(ChannelMessage.builder()
                .channel(channel)
                .message(message)
                .json(data)
                .targetNode(this.nodeInfo.getUniqueId())
                .build());
    }

    @Override
    public boolean isConnected() {
        return this.channel != null;
    }

    @Override
    public void saveSendPacket(@NotNull IPacket packet) {
        if (this.channel != null) {
            this.channel.sendPacket(packet);
        }

    }

    @Override
    public boolean isAcceptableConnection(@NotNull INetworkChannel channel, @NotNull String nodeId) {
        return this.channel == null && this.nodeInfo.getUniqueId().equals(nodeId);
    }

    @Override
    public String[] sendCommandLine(@NotNull String commandLine) {
        if (this.channel != null) {
            return this.executeDriverAPIMethod(
                    DriverAPIRequestType.SEND_COMMAND_LINE,
                    buffer -> buffer.writeString(commandLine),
                    packet -> packet.getBuffer().readStringArray()
            ).get(5, TimeUnit.SECONDS, null);
        }

        return null;
    }

    @Override
    public void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull byte[] zipResource) {
        this.saveSendPacket(new PacketServerDeployLocalTemplate(serviceTemplate, zipResource, true));
    }

    @Override
    public CloudServiceFactory getCloudServiceFactory() {
        return this.cloudServiceFactory;
    }

    @Override
    public SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        return new RemoteSpecificCloudServiceProvider(this.channel, serviceInfoSnapshot);
    }

    @Override
    public void close() throws Exception {
        if (this.channel != null) {
            this.channel.close();
        }

        this.nodeInfoSnapshot = null;
        this.channel = null;
    }

    @NotNull
    public DefaultClusterNodeServerProvider getProvider() {
        return this.provider;
    }

    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot() {
        return this.nodeInfoSnapshot;
    }

    public void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot) {
        this.nodeInfoSnapshot = nodeInfoSnapshot;
    }

    @Override
    public INetworkChannel getChannel() {
        return this.channel;
    }

    @NotNull
    public NetworkClusterNode getNodeInfo() {
        return this.nodeInfo;
    }

    public void setNodeInfo(@NotNull NetworkClusterNode nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    @Override
    public INetworkChannel getNetworkChannel() {
        return this.channel;
    }

    public void setChannel(@NotNull INetworkChannel channel) {
        this.channel = channel;
    }
}