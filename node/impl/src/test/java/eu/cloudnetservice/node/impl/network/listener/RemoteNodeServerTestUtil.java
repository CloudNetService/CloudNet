/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.network.listener;

import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry;
import eu.cloudnetservice.driver.network.rpc.factory.RPCImplementationBuilder;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.impl.cluster.defaults.RemoteNodeServer;
import eu.cloudnetservice.node.impl.cluster.util.NodeDisconnectHandler;
import lombok.NonNull;
import org.mockito.Mockito;

final class RemoteNodeServerTestUtil {

  private RemoteNodeServerTestUtil() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  static @NonNull RemoteNodeServer createRemoteNodeServer(
    @NonNull NetworkClusterNode node,
    @NonNull NodeServerProvider provider,
    @NonNull NodeDisconnectHandler disconnectHandler
  ) {
    var allocator = (RPCImplementationBuilder.InstanceAllocator<CloudServiceFactory>) Mockito.mock(
      RPCImplementationBuilder.InstanceAllocator.class);
    Mockito.when(allocator.withTargetChannel(Mockito.any())).thenReturn(allocator);
    Mockito.when(allocator.allocate()).thenReturn(Mockito.mock(CloudServiceFactory.class));
    return new RemoteNodeServer(
      node,
      provider,
      Mockito.mock(NetworkClient.class),
      Mockito.mock(DataSyncRegistry.class),
      disconnectHandler,
      Mockito.mock(CloudServiceProvider.class),
      allocator);
  }

  static void initSession(
    @NonNull RemoteNodeServer server,
    @NonNull NetworkChannel channel,
    long startupMillis,
    @NonNull NodeServerState state
  ) {
    var snapshot = Mockito.mock(NodeInfoSnapshot.class);
    Mockito.when(snapshot.startupMillis()).thenReturn(startupMillis);
    server.channel(channel);
    server.updateNodeInfoSnapshot(snapshot);
    server.state(state);
  }

  static @NonNull NetworkChannel mockChannel() {
    var channel = Mockito.mock(NetworkChannel.class);
    Mockito.when(channel.packetRegistry()).thenReturn(Mockito.mock(PacketListenerRegistry.class));
    return channel;
  }
}
