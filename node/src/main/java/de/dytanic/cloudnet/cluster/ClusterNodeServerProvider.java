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

import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.chunk.TransferStatus;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.protocol.PacketSender;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.io.InputStream;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Represents the full management of all nodes of the cluster. It's manage all nodes that are configured on the
 * platform
 */
public interface ClusterNodeServerProvider extends NodeServerProvider<ClusterNodeServer>, PacketSender, AutoCloseable {

  /**
   * Returns the node with the specific channel that is configured
   *
   * @param channel the channel, that the node is connected with
   * @return the IClusterNodeServer instance or null if the node doesn't registered
   */
  @Nullable ClusterNodeServer nodeServer(@NonNull NetworkChannel channel);

  /**
   * Set, replace or update all cluster nodes that are configured
   *
   * @param networkCluster the specific cluster network node configuration, that can create new IClusterNodeServer
   *                       instances
   */
  void clusterServers(@NonNull NetworkCluster networkCluster);

  @NonNull
  Task<TransferStatus> deployTemplateToCluster(@NonNull ServiceTemplate template, @NonNull InputStream stream,
    boolean overwrite);

  @NonNull
  Task<TransferStatus> deployStaticServiceToCluster(@NonNull String name, @NonNull InputStream stream,
    boolean overwrite);

  /**
   * Get all node server network channels which are currently connected and recognized by this provider.
   *
   * @return all node server network channels which are currently connected.
   */
  @UnmodifiableView
  @NonNull Collection<NetworkChannel> connectedChannels();

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

  void syncClusterData();
}
