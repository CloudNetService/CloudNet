/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.protocol.IPacketSender;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the full management of all nodes of the cluster. It's manage all nodes that are configured on the
 * platform
 */
public interface IClusterNodeServerProvider extends NodeServerProvider<IClusterNodeServer>, IPacketSender,
  AutoCloseable {

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
   * @param networkCluster the specific cluster network node configuration, that can create new IClusterNodeServer
   *                       instances
   */
  void setClusterServers(@NotNull NetworkCluster networkCluster);

  /**
   * Deploys the given template to all connected nodes.
   *
   * @param serviceTemplate the specific template prefix and name configuration
   * @param zipResource     the template data as a zip archive
   * @deprecated use {@link #deployTemplateInCluster(ServiceTemplate, InputStream)} instead, this method causes high
   * heap usage
   */
  @Deprecated
  default void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, byte[] zipResource) {
    this.deployTemplateInCluster(serviceTemplate, new ByteArrayInputStream(zipResource));
  }

  /**
   * Deploys the given template to all connected nodes.
   *
   * @param serviceTemplate the specific template prefix and name configuration
   * @param inputStream     the template data as a zip archive
   */
  void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull InputStream inputStream);

  /**
   * Get all node server network channels which are currently connected and recognized by this provider.
   *
   * @return all node server network channels which are currently connected.
   */
  Collection<INetworkChannel> getConnectedChannels();

  /**
   * Get whether any other node is connected with this node.
   *
   * @return whether any other node is connected with this node.
   */
  boolean hasAnyConnection();

  /**
   * Checks if all nodes had sent a node snapshot update recently or disconnects them.
   */
  void checkForDeadNodes();
}
