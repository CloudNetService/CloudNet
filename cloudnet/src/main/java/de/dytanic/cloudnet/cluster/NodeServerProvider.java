package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.util.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;

/**
 * Represents an instance of a node server provider. This class is different to {@link IClusterNodeServerProvider}
 * because it recognizes all nodes instead of just the remote ones.
 *
 * @since 15. October 2020
 */
public interface NodeServerProvider {

    /**
     * @return All registered node servers in this provider.
     */
    @NotNull
    @UnmodifiableView
    Collection<NodeServer> getRegisteredNodeServers();

    /**
     * Returns the node with the specific uniqueId that is configured
     *
     * @param uniqueId the uniqueId of the node to get.
     * @return the NodeServer instance or null if the node isn't registered
     */
    @Nullable
    NodeServer getNodeServer(@NotNull String uniqueId);

    /**
     * Get the identity of the cluster head node.
     *
     * @return the identity of the cluster head node.
     */
    @NotNull
    Identity<NodeServer> getHeadNode();

    /**
     * Get the node server of this node.
     *
     * @return the node server of this node.
     */
    @NotNull
    NodeServer getCurrentNodeServer();
}
