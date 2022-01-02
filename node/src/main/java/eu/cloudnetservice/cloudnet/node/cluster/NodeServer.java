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

import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.cloudnet.driver.provider.service.CloudServiceFactory;
import eu.cloudnetservice.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

@RPCValidation
public interface NodeServer extends AutoCloseable {

  @NonNull NodeServerProvider<? extends NodeServer> provider();

  boolean headNode();

  boolean available();

  boolean drain();

  void drain(boolean drain);

  @NonNull NetworkClusterNode nodeInfo();

  @Internal
  void nodeInfo(@NonNull NetworkClusterNode nodeInfo);

  @UnknownNullability NetworkClusterNodeInfoSnapshot nodeInfoSnapshot();

  @Internal
  void nodeInfoSnapshot(@NonNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot);

  @UnknownNullability NetworkClusterNodeInfoSnapshot lastNodeInfoSnapshot();

  @NonNull
  @UnmodifiableView Collection<String> sendCommandLine(@NonNull String commandLine);

  @NonNull CloudServiceFactory cloudServiceFactory();

  @Nullable SpecificCloudServiceProvider cloudServiceProvider(@NonNull ServiceInfoSnapshot serviceInfoSnapshot);
}
