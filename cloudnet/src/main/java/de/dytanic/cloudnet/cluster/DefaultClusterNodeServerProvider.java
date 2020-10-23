package de.dytanic.cloudnet.cluster;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacketBuilder;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class DefaultClusterNodeServerProvider extends DefaultNodeServerProvider implements IClusterNodeServerProvider {

    protected final Map<String, IClusterNodeServer> servers = new ConcurrentHashMap<>();

    @Override
    public @NotNull Collection<IClusterNodeServer> getNodeServers() {
        return this.servers.values();
    }

    @Nullable
    @Override
    public IClusterNodeServer getNodeServer(@NotNull String uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.servers.get(uniqueId);
    }

    @Override
    public IClusterNodeServer getNodeServer(@NotNull INetworkChannel channel) {
        Preconditions.checkNotNull(channel);

        for (IClusterNodeServer clusterNodeServer : this.servers.values()) {
            if (clusterNodeServer.getNetworkChannel() != null && clusterNodeServer.getNetworkChannel().getChannelId() == channel.getChannelId()) {
                return clusterNodeServer;
            }
        }

        return null;
    }

    @Override
    public void setClusterServers(@NotNull NetworkCluster networkCluster) {
        for (NetworkClusterNode clusterNode : networkCluster.getNodes()) {
            if (this.servers.containsKey(clusterNode.getUniqueId())) {
                this.servers.get(clusterNode.getUniqueId()).setNodeInfo(clusterNode);
            } else {
                IClusterNodeServer server = new DefaultClusterNodeServer(this, clusterNode);
                super.registerNode(server);
                this.servers.put(clusterNode.getUniqueId(), server);
            }
        }

        for (IClusterNodeServer clusterNodeServer : this.servers.values()) {
            NetworkClusterNode node = networkCluster.getNodes().stream()
                    .filter(networkClusterNode -> networkClusterNode.getUniqueId().equalsIgnoreCase(clusterNodeServer.getNodeInfo().getUniqueId()))
                    .findFirst().orElse(null);

            if (node == null) {
                this.servers.remove(clusterNodeServer.getNodeInfo().getUniqueId());
                super.unregisterNode(clusterNodeServer.getNodeInfo().getUniqueId());
            }
        }
    }

    @Override
    public void sendPacket(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        for (IClusterNodeServer nodeServer : this.servers.values()) {
            nodeServer.saveSendPacket(packet);
        }
    }

    @Override
    public void sendPacketSync(@NotNull IPacket packet) {
        Preconditions.checkNotNull(packet);

        for (IClusterNodeServer server : this.servers.values()) {
            if (server.getNetworkChannel() != null) {
                server.getNetworkChannel().sendPacketSync(packet);
            }
        }
    }

    @Override
    public void sendPacket(@NotNull IPacket... packets) {
        Preconditions.checkNotNull(packets);

        for (IPacket packet : packets) {
            this.sendPacket(packet);
        }
    }

    @Override
    public void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull byte[] zipResource) {
        this.deployTemplateInCluster(serviceTemplate, new ByteArrayInputStream(zipResource));
    }

    @Override
    public void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull InputStream inputStream) {
        if (this.servers.values().stream().noneMatch(IClusterNodeServer::isConnected)) {
            return;
        }

        Collection<INetworkChannel> channels = this.servers
                .values()
                .stream()
                .map(IClusterNodeServer::getNetworkChannel)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        try {
            JsonDocument header = JsonDocument.newDocument().append("template", serviceTemplate).append("preClear", true);

            ChunkedPacketBuilder.newBuilder(PacketConstants.CLUSTER_TEMPLATE_DEPLOY_CHANNEL, inputStream)
                    .header(header)
                    .target(channels)
                    .complete();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        for (IClusterNodeServer clusterNodeServer : this.servers.values()) {
            clusterNodeServer.close();
        }

        this.servers.clear();
    }

    public Map<String, IClusterNodeServer> getServers() {
        return this.servers;
    }
}