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

import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NodeServerProvider<T extends NodeServer> {

  /**
   * Returns the represent nodes that are configured on the application. The nodes shouldn't be online
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
