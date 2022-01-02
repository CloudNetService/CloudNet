/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.cluster;

import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface NodeServerProvider<T extends NodeServer> {

  /**
   * Returns the represented nodes that are configured on the application. The nodes may be online, but they don't have
   * to
   */
  @UnmodifiableView
  @NonNull Collection<T> nodeServers();

  /**
   * Returns the node with the specific uniqueId that is configured
   *
   * @param uniqueId the uniqueId from the node, that should retrieve
   * @return the IClusterNodeServer instance or null if the node doesn't registered
   */
  @Nullable T nodeServer(@NonNull String uniqueId);

  /**
   * Gets the current head node of the cluster, may be the local node.
   *
   * @return the current head node of the cluster.
   */
  @NonNull NodeServer headnode();

  /**
   * Get the jvm static local node server implementation.
   *
   * @return the jvm static local node server implementation.
   */
  @NonNull NodeServer selfNode();

  /**
   * Re-calculates the head node of the current cluster.
   */
  void refreshHeadNode();
}
