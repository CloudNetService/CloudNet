package de.dytanic.cloudnet.cluster;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import org.jetbrains.annotations.NotNull;

public abstract class DefaultNodeServer implements NodeServer {

  protected volatile NetworkClusterNode nodeInfo;
  protected volatile NetworkClusterNodeInfoSnapshot lastSnapshot;
  protected volatile NetworkClusterNodeInfoSnapshot currentSnapshot;

  @Override
  public boolean isHeadNode() {
    return this.getProvider().getHeadNode() == this;
  }

  @Override
  public @NotNull NetworkClusterNode getNodeInfo() {
    return this.nodeInfo;
  }

  @Override
  public void setNodeInfo(@NotNull NetworkClusterNode nodeInfo) {
    this.nodeInfo = Preconditions.checkNotNull(nodeInfo, "nodeInfo");
  }

  @Override
  public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot() {
    return this.currentSnapshot;
  }

  @Override
  public void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot) {
    Preconditions.checkNotNull(nodeInfoSnapshot, "nodeInfoSnapshot");

    this.lastSnapshot = this.currentSnapshot == null ? nodeInfoSnapshot : this.currentSnapshot;
    this.currentSnapshot = nodeInfoSnapshot;
  }

  @Override
  public NetworkClusterNodeInfoSnapshot getLastNodeInfoSnapshot() {
    return this.lastSnapshot;
  }

  @Override
  public void close() throws Exception {
    this.getProvider().refreshHeadNode();
  }
}
