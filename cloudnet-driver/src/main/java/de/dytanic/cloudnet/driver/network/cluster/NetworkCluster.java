package de.dytanic.cloudnet.driver.network.cluster;

import java.util.Collection;
import java.util.UUID;

public class NetworkCluster {

    private UUID clusterId;

    private Collection<NetworkClusterNode> nodes;

    public NetworkCluster(UUID clusterId, Collection<NetworkClusterNode> nodes) {
        this.clusterId = clusterId;
        this.nodes = nodes;
    }

    public NetworkCluster() {
    }

    public UUID getClusterId() {
        return this.clusterId;
    }

    public Collection<NetworkClusterNode> getNodes() {
        return this.nodes;
    }

    public void setClusterId(UUID clusterId) {
        this.clusterId = clusterId;
    }

    public void setNodes(Collection<NetworkClusterNode> nodes) {
        this.nodes = nodes;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkCluster)) return false;
        final NetworkCluster other = (NetworkCluster) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$clusterId = this.getClusterId();
        final Object other$clusterId = other.getClusterId();
        if (this$clusterId == null ? other$clusterId != null : !this$clusterId.equals(other$clusterId)) return false;
        final Object this$nodes = this.getNodes();
        final Object other$nodes = other.getNodes();
        if (this$nodes == null ? other$nodes != null : !this$nodes.equals(other$nodes)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NetworkCluster;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $clusterId = this.getClusterId();
        result = result * PRIME + ($clusterId == null ? 43 : $clusterId.hashCode());
        final Object $nodes = this.getNodes();
        result = result * PRIME + ($nodes == null ? 43 : $nodes.hashCode());
        return result;
    }

    public String toString() {
        return "NetworkCluster(clusterId=" + this.getClusterId() + ", nodes=" + this.getNodes() + ")";
    }
}