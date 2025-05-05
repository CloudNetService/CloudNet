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

package eu.cloudnetservice.node.impl.network.listener;

import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.impl.cluster.util.QueuedNetworkChannel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class ServiceSyncAckPacketListener implements PacketListener {

  private final DataSyncRegistry dataSyncRegistry;
  private final NodeServerProvider nodeServerProvider;

  @Inject
  public ServiceSyncAckPacketListener(
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull NodeServerProvider nodeServerProvider
  ) {
    this.dataSyncRegistry = dataSyncRegistry;
    this.nodeServerProvider = nodeServerProvider;
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) throws Exception {
    // read the cluster node snapshot
    var snapshot = packet.content().readObject(NodeInfoSnapshot.class);
    var syncData = packet.content().readDataBuf();
    // select the node server and validate that it is in the right state for the packet
    var server = this.nodeServerProvider.node(snapshot.node().uniqueId());
    if (server != null && server.state() == NodeServerState.SYNCING) {
      // remove this listener
      channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_SERVICE_SYNC_ACK_CHANNEL);
      // sync the data between the nodes
      this.dataSyncRegistry.handle(syncData, syncData.readBoolean());
      if (server.channel() instanceof QueuedNetworkChannel queuedChannel) {
        queuedChannel.drainPacketQueue(channel);
      }

      // close the old channel
      // little hack to prevent some disconnect handling firring in the channel if the state was not set before
      server.state(NodeServerState.DISCONNECTED);
      server.channel().close();
      // mark the node as ready
      server.channel(channel);
      server.updateNodeInfoSnapshot(snapshot);
      server.state(NodeServerState.READY);
      // re-select the head node
      this.nodeServerProvider.selectHeadNode();
    }
  }
}
