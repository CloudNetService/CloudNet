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

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import lombok.NonNull;

public abstract class DefaultNodeServer implements NodeServer {

  protected volatile NetworkClusterNode nodeInfo;
  protected volatile NetworkClusterNodeInfoSnapshot lastSnapshot;
  protected volatile NetworkClusterNodeInfoSnapshot currentSnapshot;

  @Override
  public boolean headNode() {
    return this.provider().headnode() == this;
  }

  @Override
  public @NonNull NetworkClusterNode nodeInfo() {
    return this.nodeInfo;
  }

  @Override
  public void nodeInfo(@NonNull NetworkClusterNode nodeInfo) {
    this.nodeInfo = nodeInfo;
  }

  @Override
  public NetworkClusterNodeInfoSnapshot nodeInfoSnapshot() {
    return this.currentSnapshot;
  }

  @Override
  public void nodeInfoSnapshot(@NonNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot) {
    this.lastSnapshot = this.currentSnapshot == null ? nodeInfoSnapshot : this.currentSnapshot;
    this.currentSnapshot = nodeInfoSnapshot;
  }

  @Override
  public NetworkClusterNodeInfoSnapshot lastNodeInfoSnapshot() {
    return this.lastSnapshot;
  }

  @Override
  public void close() throws Exception {
    this.provider().refreshHeadNode();
  }
}
