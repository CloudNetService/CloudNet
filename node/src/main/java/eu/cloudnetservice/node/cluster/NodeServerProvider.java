/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.cluster;

import eu.cloudnetservice.driver.cluster.NetworkCluster;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.protocol.PacketSender;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import java.io.Closeable;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface NodeServerProvider extends PacketSender, Closeable {

  @NonNull
  Collection<NodeServer> nodeServers();

  @NonNull
  Collection<NodeServer> availableNodeServers();

  @NonNull
  Collection<NetworkChannel> connectedNodeChannels();

  @NonNull
  NodeServer headNode();

  @NonNull
  LocalNodeServer localNode();

  @Nullable
  NodeServer node(@NonNull String uniqueId);

  @Nullable
  NodeServer node(@NonNull NetworkChannel channel);

  void syncDataIntoCluster();

  void registerNodes(@NonNull NetworkCluster cluster);

  void registerNode(@NonNull NetworkClusterNode clusterNode);

  void unregisterNode(@NonNull String uniqueId);

  void selectHeadNode();

  @NonNull
  CompletableFuture<TransferStatus> deployTemplateToCluster(
    @NonNull ServiceTemplate template,
    @NonNull InputStream stream,
    boolean overwrite);

  @NonNull
  CompletableFuture<TransferStatus> deployStaticServiceToCluster(
    @NonNull String name,
    @NonNull InputStream stream,
    boolean overwrite);

  @Override
  void close();
}
