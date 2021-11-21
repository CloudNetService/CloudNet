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
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCValidation;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

@RPCValidation
public interface NodeServer extends AutoCloseable {

  @NotNull NodeServerProvider<? extends NodeServer> getProvider();

  boolean isHeadNode();

  boolean isAvailable();

  boolean isDrain();

  void setDrain(boolean drain);

  @NotNull NetworkClusterNode getNodeInfo();

  @Internal
  void setNodeInfo(@NotNull NetworkClusterNode nodeInfo);

  @UnknownNullability NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot();

  @Internal
  void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot);

  @UnknownNullability NetworkClusterNodeInfoSnapshot getLastNodeInfoSnapshot();

  @NotNull
  @UnmodifiableView Collection<String> sendCommandLine(@NotNull String commandLine);

  @NotNull CloudServiceFactory getCloudServiceFactory();

  @Nullable SpecificCloudServiceProvider getCloudServiceProvider(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);
}
