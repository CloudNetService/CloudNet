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
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NodeServer extends AutoCloseable {

  @NotNull
  NodeServerProvider<? extends NodeServer> getProvider();

  boolean isHeadNode();

  boolean isAvailable();

  @NotNull
  NetworkClusterNode getNodeInfo();

  @ApiStatus.Internal
  void setNodeInfo(@NotNull NetworkClusterNode nodeInfo);

  NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot();

  @ApiStatus.Internal
  void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot);

  NetworkClusterNodeInfoSnapshot getLastNodeInfoSnapshot();

  @NotNull
  String[] sendCommandLine(@NotNull String commandLine);

  @NotNull
  CloudServiceFactory getCloudServiceFactory();

  @Nullable
  SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);
}
