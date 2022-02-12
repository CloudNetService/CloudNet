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

import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.cloudnet.driver.provider.SpecificCloudServiceProvider;
import java.io.Closeable;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public interface NodeServer extends Nameable, Closeable {

  boolean head();

  boolean available();

  void shutdown();

  boolean connect();

  boolean draining();

  void drain(boolean doDrain);

  void syncClusterData(boolean force);

  @NonNull NetworkClusterNode info();

  @NonNull NodeServerProvider provider();

  @NonNull NodeServerState state();

  void state(@NonNull NodeServerState state);

  @NonNull Instant lastStateChangeStamp();

  @UnknownNullability NetworkChannel channel();

  void channel(@Nullable NetworkChannel channel);

  @UnknownNullability NetworkClusterNodeInfoSnapshot nodeInfoSnapshot();

  @UnknownNullability NetworkClusterNodeInfoSnapshot lastNodeInfoSnapshot();

  void updateNodeInfoSnapshot(@Nullable NetworkClusterNodeInfoSnapshot snapshot);

  @NonNull CloudServiceFactory serviceFactory();

  @Nullable SpecificCloudServiceProvider serviceProvider(@NonNull UUID uniqueId);

  @NonNull Collection<String> sendCommandLine(@NonNull String commandLine);

  @Override
  void close();
}
