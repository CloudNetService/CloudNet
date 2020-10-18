package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface NodeServer extends AutoCloseable {

    @NotNull
    NodeServerProvider getProvider();

    boolean isHeadNode();

    @NotNull
    NetworkClusterNode getNodeInfo();

    void setNodeInfo(@NotNull NetworkClusterNode nodeInfo);

    NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot();

    void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot);

    String[] sendCommandLine(@NotNull String commandLine);

    @ApiStatus.Internal
    CloudServiceFactory getCloudServiceFactory();

    @ApiStatus.Internal
    SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);
}
