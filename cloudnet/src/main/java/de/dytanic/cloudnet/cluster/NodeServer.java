package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NodeServer extends AutoCloseable {

  @NotNull
  NodeServerProvider<? extends NodeServer> getProvider();

  boolean isHeadNode();

  boolean isAvailable();

  @NotNull
  NetworkClusterNode getNodeInfo();

  @ApiStatus.Internal
  void setNodeInfo(@NotNull NetworkClusterNode nodeInfo);

  NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot();

  @ApiStatus.Internal
  void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot);

  NetworkClusterNodeInfoSnapshot getLastNodeInfoSnapshot();

  @NotNull
  String[] sendCommandLine(@NotNull String commandLine);

  @NotNull
  CloudServiceFactory getCloudServiceFactory();

  @Nullable
  SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);
}
