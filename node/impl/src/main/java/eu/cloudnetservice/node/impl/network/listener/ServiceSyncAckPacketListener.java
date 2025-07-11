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
    try {
      var packetContent = packet.content();
      var snapshot = packetContent.readObject(NodeInfoSnapshot.class);
      var server = this.nodeServerProvider.node(snapshot.node().uniqueId());
      if (server != null && server.state() == NodeServerState.SYNCING) {
        try (var syncData = packet.content().readDataBuf()) {
          var forceApply = syncData.readBoolean();
          this.dataSyncRegistry.handle(syncData, forceApply);
        }

        // flush the packets that were queued for the node that reconnected
        if (server.channel() instanceof QueuedNetworkChannel queuedChannel) {
          queuedChannel.drainPacketQueue(channel);
        }

        // closes the old channel, preventing disconnection handling by setting the state
        // of the channel to 'disconnected' before actually closing the channel
        server.state(NodeServerState.DISCONNECTED);
        server.channel().close();

        // mark the node as ready and re-select the head node. this ensures that the
        // current node uses the same node as the head node as all other nodes in the cluster
        server.channel(channel);
        server.updateNodeInfoSnapshot(snapshot);
        server.state(NodeServerState.READY);
        this.nodeServerProvider.selectHeadNode();
      }
    } finally {
      // the packet is only sent once, this listener can be removed
      channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_SERVICE_SYNC_ACK_CHANNEL);
    }
  }
}
