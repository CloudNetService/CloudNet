package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.protocol.IPacketSender;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collection;

/**
 * Represents the full management of all nodes of the cluster.
 * It's manage all nodes that are configured on the platform
 */
public interface IClusterNodeServerProvider extends AutoCloseable, IPacketSender {

    /**
     * Returns the represent nodes that are configured on the application.
     * The nodes shouldn't be online
     */
    Collection<IClusterNodeServer> getNodeServers();

    /**
     * Returns the node with the specific uniqueId that is configured
     *
     * @param uniqueId the uniqueId from the node, that should retrieve
     * @return the IClusterNodeServer instance or null if the node doesn't registered
     */
    @Nullable
    IClusterNodeServer getNodeServer(@NotNull String uniqueId);

    /**
     * Returns the node with the specific channel that is configured
     *
     * @param channel the channel, that the node is connected with
     * @return the IClusterNodeServer instance or null if the node doesn't registered
     */
    @Nullable
    IClusterNodeServer getNodeServer(@NotNull INetworkChannel channel);

    /**
     * Set, replace or update all cluster nodes that are configured
     *
     * @param networkCluster the specific cluster network node configuration, that can create new IClusterNodeServer instances
     */
    void setClusterServers(@NotNull NetworkCluster networkCluster);

    /**
     * Deploys the given template to all connected nodes.
     *
     * @param serviceTemplate the specific template prefix and name configuration
     * @param zipResource     the template data as a zip archive
     * @deprecated use {@link #deployTemplateInCluster(ServiceTemplate, InputStream)} instead, this method causes high heap usage
     */
    @Deprecated
    void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull byte[] zipResource);

    /**
     * Deploys the given template to all connected nodes.
     *
     * @param serviceTemplate the specific template prefix and name configuration
     * @param inputStream     the template data as a zip archive
     */
    void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull InputStream inputStream);

}