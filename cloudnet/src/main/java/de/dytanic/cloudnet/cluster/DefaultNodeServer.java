package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import org.jetbrains.annotations.NotNull;

public abstract class DefaultNodeServer implements NodeServer {

    protected final NodeServerProvider provider;
    protected volatile NetworkClusterNodeInfoSnapshot lastInfoSnapshot;
    protected volatile NetworkClusterNodeInfoSnapshot infoSnapshot;
    protected NetworkClusterNode nodeInfo;

    protected DefaultNodeServer(NodeServerProvider provider, NetworkClusterNode nodeInfo) {
        this.provider = provider;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public @NotNull NodeServerProvider getProvider() {
        return this.provider;
    }

    @Override
    public boolean isHeadNode() {
        return this.provider.getHeadNode().isInstance(this);
    }

    @Override
    public @NotNull NetworkClusterNode getNodeInfo() {
        return this.nodeInfo;
    }

    @Override
    public void setNodeInfo(@NotNull NetworkClusterNode nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    @Override
    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot() {
        return this.infoSnapshot;
    }

    @Override
    public NetworkClusterNodeInfoSnapshot getLastNodeInfoSnapshot() {
        return this.lastInfoSnapshot;
    }

    @Override
    public void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot) {
        this.lastInfoSnapshot = this.infoSnapshot == null ? nodeInfoSnapshot : this.infoSnapshot;
        this.infoSnapshot = nodeInfoSnapshot;
    }

    @Override
    public void close() throws Exception {
        this.infoSnapshot = null;
    }
}
