package de.dytanic.cloudnet.cluster;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacketBuilder;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DefaultClusterNodeServerProvider extends DefaultNodeServerProvider<IClusterNodeServer> implements IClusterNodeServerProvider {

    private static final long MAX_NO_UPDATE_MILLIS = Long.getLong("cloudnet.max_node_idle_millis", 30_000);

    public DefaultClusterNodeServerProvider(CloudNet cloudNet) {
        super(cloudNet);
    }

    @Override
    public IClusterNodeServer getNodeServer(@NotNull INetworkChannel channel) {
        Preconditions.checkNotNull(channel);

        for (IClusterNodeServer clusterNodeServer : this.getNodeServers()) {
            if (clusterNodeServer.getChannel() != null && clusterNodeServer.getChannel().getChannelId() == channel.getChannelId()) {
                return clusterNodeServer;
            }
        }

        return null;
    }

    @Override
    public void setClusterServers(@NotNull NetworkCluster networkCluster) {
        for (NetworkClusterNode clusterNode : networkCluster.getNodes()) {
            NodeServer nodeServer = this.getNodeServer(clusterNode.getUniqueId());
            if (nodeServer != null) {
                nodeServer.setNodeInfo(clusterNode);
            } else {
                this.nodeServers.add(new DefaultClusterNodeServer(this, clusterNode));
            }
        }

        for (IClusterNodeServer clusterNodeServer : this.nodeServers) {
            NetworkClusterNode node = networkCluster.getNodes()
                    .stream()
                    .filter(networkClusterNode -> networkClusterNode.getUniqueId().equalsIgnoreCase(clusterNodeServer.getNodeInfo().getUniqueId()))
                    .findFirst()
                    .orElse(null);
            if (node == null) {
                this.nodeServers.removeIf(n -> n.getNodeInfo().getUniqueId().equals(clusterNodeServer.getNodeInfo().getUniqueId()));
            }
        }
    }

    @Override
    public void sendPacket(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        for (IClusterNodeServer nodeServer : this.nodeServers) {
            nodeServer.saveSendPacket(packet);
        }
    }

    @Override
    public void sendPacketSync(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        for (IClusterNodeServer nodeServer : this.nodeServers) {
            nodeServer.saveSendPacketSync(packet);
        }
    }

    @Override
    public void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull InputStream inputStream) {
        if (!this.nodeServers.isEmpty()) {
            Collection<INetworkChannel> channels = this.nodeServers
                    .stream()
                    .filter(IClusterNodeServer::isConnected)
                    .map(IClusterNodeServer::getChannel)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            try {
                JsonDocument header = JsonDocument.newDocument()
                        .append("template", serviceTemplate)
                        .append("preClear", true);

                ChunkedPacketBuilder.newBuilder(PacketConstants.CLUSTER_TEMPLATE_DEPLOY_CHANNEL, inputStream)
                        .header(header)
                        .target(channels)
                        .complete();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void checkForDeadNodes() {
        for (IClusterNodeServer nodeServer : this.nodeServers) {
            if (nodeServer.isAvailable()) {
                NetworkClusterNodeInfoSnapshot snapshot = nodeServer.getNodeInfoSnapshot();
                if (snapshot != null && snapshot.getCreationTime() + MAX_NO_UPDATE_MILLIS < System.currentTimeMillis()) {
                    try {
                        nodeServer.close();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (IClusterNodeServer clusterNodeServer : this.nodeServers) {
            clusterNodeServer.close();
        }

        this.nodeServers.clear();
        this.refreshHeadNode();
    }

    /**
     * Gets all nodes servers as unique-id - server map.
     *
     * @return all nodes servers as unique-id - server map.
     * @deprecated currently mapped, use {@link #getNodeServers()} instead.
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public Map<String, IClusterNodeServer> getServers() {
        return this.nodeServers.stream().collect(Collectors.toMap(server -> server.getNodeInfo().getUniqueId(), Function.identity()));
    }
}