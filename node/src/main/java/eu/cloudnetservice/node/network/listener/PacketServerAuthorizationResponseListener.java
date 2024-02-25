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

package eu.cloudnetservice.node.network.listener;

import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.cluster.util.QueuedNetworkChannel;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.network.NodeNetworkUtil;
import eu.cloudnetservice.node.network.packet.PacketServerServiceSyncAckPacket;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Objects;
import lombok.NonNull;

@Singleton
public final class PacketServerAuthorizationResponseListener implements PacketListener {

  private static final Logger LOGGER = LogManager.logger(PacketServerAuthorizationResponseListener.class);

  private final Configuration configuration;
  private final NodeNetworkUtil networkUtil;
  private final DataSyncRegistry dataSyncRegistry;
  private final NodeServerProvider nodeServerProvider;

  @Inject
  public PacketServerAuthorizationResponseListener(
    @NonNull Configuration configuration,
    @NonNull NodeNetworkUtil networkUtil,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull NodeServerProvider nodeServerProvider
  ) {
    this.configuration = configuration;
    this.networkUtil = networkUtil;
    this.dataSyncRegistry = dataSyncRegistry;
    this.nodeServerProvider = nodeServerProvider;
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // check if the auth was successful
    if (packet.content().readBoolean()) {
      // search for the node to which the auth succeeded
      var server = this.configuration.clusterConfig().nodes().stream()
        .filter(node -> node.listeners().stream().anyMatch(host -> channel.serverAddress().equals(host)))
        .map(node -> this.nodeServerProvider.node(node.uniqueId()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
      if (server != null) {
        // check if this was a reconnection from the point of view of the other node
        if (packet.content().readBoolean()) {
          // handle the data sync
          var syncData = packet.content().readDataBuf();
          this.dataSyncRegistry.handle(syncData, syncData.readBoolean());

          // check if there are pending packets for the node
          if (server.channel() instanceof QueuedNetworkChannel queuedChannel) {
            queuedChannel.drainPacketQueue(channel);
          }

          // update the current local snapshot
          var local = this.nodeServerProvider.localNode();
          local.updateLocalSnapshot();

          // acknowledge the packet
          var data = this.dataSyncRegistry.prepareClusterData(
            true,
            DataSyncHandler::alwaysForceApply);
          channel.sendPacketSync(new PacketServerServiceSyncAckPacket(local.nodeInfoSnapshot(), data));

          // close the old channel
          // little hack to prevent some disconnect handling firring in the channel if the state was not set before
          server.state(NodeServerState.DISCONNECTED);
          server.channel().close();
        }
        // update the node status
        server.channel(channel);
        server.state(NodeServerState.READY);
        // add the packet listeners
        channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
        this.networkUtil.addDefaultPacketListeners(channel.packetRegistry());
        // we are good to go :)
        return;
      }
    }

    channel.close();
    LOGGER.warning(I18n.trans("cluster-server-networking-authorization-failed", channel.serverAddress()));
  }
}
