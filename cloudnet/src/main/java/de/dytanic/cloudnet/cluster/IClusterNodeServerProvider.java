package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.protocol.IPacketSender;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Collection;

/**
 * Represents the full management of all nodes of the cluster. It's manage all
 * nodes that are configured on the platform
 */
public interface IClusterNodeServerProvider extends AutoCloseable,
    IPacketSender {

  /**
   * Returns the represent nodes that are configured on the application. The
   * nodes shouldn't be online
   */
  Collection<IClusterNodeServer> getNodeServers();

  /**
   * Returns the node with the specific uniqueId that is configured
   *
   * @param uniqueId the uniqueId from the node, that should retrieve
   * @return the IClusterNodeServer instance or null if the node doesn't
   * registered
   */
  IClusterNodeServer getNodeServer(String uniqueId);

  /**
   * Returns the node with the specific channel that is configured
   *
   * @param channel the channel, that the node is connected with
   * @return the IClusterNodeServer instance or null if the node doesn't
   * registered
   */
  IClusterNodeServer getNodeServer(INetworkChannel channel);

  /**
   * Set, replace or update all cluster nodes that are configured
   *
   * @param networkCluster the specific cluster network node configuration, that
   * can create new IClusterNodeServer instances
   */
  void setClusterServers(NetworkCluster networkCluster);

  /**
   * Deploys to all online nodes a packet with an zip byte array resource.
   *
   * @param serviceTemplate the specific template prefix and name configuration
   * @param zipResource the template data as zip archive resource
   */
  void deployTemplateInCluster(ServiceTemplate serviceTemplate,
      byte[] zipResource);
}