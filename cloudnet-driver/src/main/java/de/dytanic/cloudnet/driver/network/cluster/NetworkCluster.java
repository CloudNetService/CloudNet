package de.dytanic.cloudnet.driver.network.cluster;

import java.util.Collection;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
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

  public void setClusterId(UUID clusterId) {
    this.clusterId = clusterId;
  }

  public Collection<NetworkClusterNode> getNodes() {
    return this.nodes;
  }

  public void setNodes(Collection<NetworkClusterNode> nodes) {
    this.nodes = nodes;
  }

}
