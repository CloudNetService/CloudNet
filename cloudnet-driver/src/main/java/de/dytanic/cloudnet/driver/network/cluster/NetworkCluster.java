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

package de.dytanic.cloudnet.driver.network.cluster;

import java.util.Collection;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class NetworkCluster {

  private final UUID clusterId;
  private final Collection<NetworkClusterNode> nodes;

  public NetworkCluster(UUID clusterId, Collection<NetworkClusterNode> nodes) {
    this.clusterId = clusterId;
    this.nodes = nodes;
  }

  public UUID getClusterId() {
    return this.clusterId;
  }

  public Collection<NetworkClusterNode> getNodes() {
    return this.nodes;
  }
}
