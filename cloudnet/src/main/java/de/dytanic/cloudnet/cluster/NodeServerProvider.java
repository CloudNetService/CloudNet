package de.dytanic.cloudnet.cluster;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface NodeServerProvider<T extends NodeServer> {

    /**
     * Returns the represent nodes that are configured on the application.
     * The nodes shouldn't be online
     */
    Collection<T> getNodeServers();

    /**
     * Returns the node with the specific uniqueId that is configured
     *
     * @param uniqueId the uniqueId from the node, that should retrieve
     * @return the IClusterNodeServer instance or null if the node doesn't registered
     */
    @Nullable
    T getNodeServer(@NotNull String uniqueId);

    /**
     * Gets the current head node of the cluster, may be the local node.
     *
     * @return the current head node of the cluster.
     */
    NodeServer getHeadNode();

    /**
     * Get the jvm static local node server implementation.
     *
     * @return the jvm static local node server implementation.
     */
    NodeServer getSelfNode();

    /**
     * Re-calculates the head node of the current cluster.
     */
    void refreshHeadNode();
}
