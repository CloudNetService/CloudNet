package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.network.packet.PacketServerDeployLocalTemplate;

import java.util.Collection;
import java.util.Map;

public final class DefaultClusterNodeServerProvider implements IClusterNodeServerProvider {

    protected final Map<String, ClusterNodeServer> servers = Maps.newConcurrentHashMap();

    @Override
    public Collection<ClusterNodeServer> getNodeServers() {
//        return this.servers.values().stream().map((server) -> (IClusterNodeServer) server).collect(Collectors.toCollection(LinkedList::new));
        return this.servers.values();
    }

    @Override
    public ClusterNodeServer getNodeServer(String uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.servers.get(uniqueId);
    }

    @Override
    public ClusterNodeServer getNodeServer(INetworkChannel channel) {
        Validate.checkNotNull(channel);

        for (ClusterNodeServer clusterNodeServer : this.servers.values()) {
            if (clusterNodeServer.getChannel() != null && clusterNodeServer.getChannel().getChannelId() == channel.getChannelId()) {
                return clusterNodeServer;
            }
        }

        return null;
    }

    @Override
    public void setClusterServers(NetworkCluster networkCluster) {
        for (NetworkClusterNode clusterNode : networkCluster.getNodes()) {
            if (this.servers.containsKey(clusterNode.getUniqueId())) {
                this.servers.get(clusterNode.getUniqueId()).setNodeInfo(clusterNode);
            } else {
                this.servers.put(clusterNode.getUniqueId(), new DefaultClusterNodeServer(this, clusterNode));
            }
        }

        for (ClusterNodeServer clusterNodeServer : this.servers.values()) {
            NetworkClusterNode node = Iterables.first(networkCluster.getNodes(), networkClusterNode -> networkClusterNode.getUniqueId().equalsIgnoreCase(clusterNodeServer.getNodeInfo().getUniqueId()));

            if (node == null) {
                this.servers.remove(clusterNodeServer.getNodeInfo().getUniqueId());
            }
        }
    }

    @Override
    public void sendPacket(Packet packet) {
        Validate.checkNotNull(packet);

        for (ClusterNodeServer nodeServer : this.servers.values()) {
            nodeServer.saveSendPacket(packet);
        }
    }

    @Override
    public void sendPacket(Packet... packets) {
        Validate.checkNotNull(packets);

        for (Packet packet : packets) {
            this.sendPacket(packet);
        }
    }

    @Override
    public void deployTemplateInCluster(ServiceTemplate serviceTemplate, byte[] zipResource) {
        this.sendPacket(new PacketServerDeployLocalTemplate(serviceTemplate, zipResource, true));
    }

    @Override
    public void close() throws Exception {
        for (ClusterNodeServer clusterNodeServer : servers.values()) {
            clusterNodeServer.close();
        }

        this.servers.clear();
    }

    public Map<String, ClusterNodeServer> getServers() {
        return this.servers;
    }
}