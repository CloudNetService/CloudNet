package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultNodeServerProvider<T extends NodeServer> implements NodeServerProvider<T> {

  protected final NodeServer localNode;
  protected final Set<T> nodeServers = new CopyOnWriteArraySet<>();

  protected volatile NodeServer headNode;

  public DefaultNodeServerProvider(CloudNet cloudNet) {
    this.localNode = new LocalNodeServer(cloudNet, this);
    this.refreshHeadNode();
  }

  @Override
  public Collection<T> getNodeServers() {
    return Collections.unmodifiableCollection(this.nodeServers);
  }

  @Override
  public @Nullable T getNodeServer(@NotNull String uniqueId) {
    for (T nodeServer : this.nodeServers) {
      if (nodeServer.getNodeInfo().getUniqueId().equals(uniqueId)) {
        return nodeServer;
      }
    }
    return null;
  }

  @Override
  public NodeServer getHeadNode() {
    return this.headNode;
  }

  @Override
  public NodeServer getSelfNode() {
    return this.localNode;
  }

  @Override
  public void refreshHeadNode() {
    NodeServer currentChoice = this.localNode;
    if (!this.nodeServers.isEmpty()) {
      for (T nodeServer : this.nodeServers) {
        if (nodeServer.isAvailable()) {
          NetworkClusterNodeInfoSnapshot snapshot = nodeServer.getNodeInfoSnapshot();
          if (snapshot != null && snapshot.getStartupMillis() < currentChoice.getNodeInfoSnapshot()
            .getStartupMillis()) {
            currentChoice = nodeServer;
          }
        }
      }
    }

    this.headNode = currentChoice;
  }
}
