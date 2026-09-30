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

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.cluster.util.QueuedNetworkChannel;
import eu.cloudnetservice.node.impl.network.NodeNetworkUtil;
import eu.cloudnetservice.node.impl.network.packet.ServiceSyncAckPacket;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Objects;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class AuthorizationResponsePacketListener implements PacketListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationResponsePacketListener.class);

  private final I18n i18n;
  private final Configuration configuration;
  private final NodeNetworkUtil networkUtil;
  private final DataSyncRegistry dataSyncRegistry;
  private final NodeServerProvider nodeServerProvider;
  private final CloudServiceManager cloudServiceManager;

  @Inject
  public AuthorizationResponsePacketListener(
    @NonNull @Service I18n i18n,
    @NonNull Configuration configuration,
    @NonNull NodeNetworkUtil networkUtil,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull CloudServiceManager cloudServiceManager
  ) {
    this.i18n = i18n;
    this.configuration = configuration;
    this.networkUtil = networkUtil;
    this.dataSyncRegistry = dataSyncRegistry;
    this.nodeServerProvider = nodeServerProvider;
    this.cloudServiceManager = cloudServiceManager;
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // for fields an order see AuthorizationResponsePacket
    var packetContent = packet.content();
    var isAuthSuccess = packetContent.readBoolean();
    if (isAuthSuccess) {
      var server = this.configuration.clusterConfig().nodes().stream()
        .filter(node -> node.listeners().stream().anyMatch(host -> channel.serverAddress().equals(host)))
        .map(node -> this.nodeServerProvider.node(node.uniqueId()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
      if (server != null) {
        var wasReconnect = packetContent.readBoolean();
        var republishServices = false;
        if (wasReconnect) {
          try (var syncData = packetContent.readDataBuf()) {
            var forceApply = syncData.readBoolean();
            this.dataSyncRegistry.handle(syncData, forceApply);
          }

          // flush the packets that were queued for the node that reconnected
          if (server.channel() instanceof QueuedNetworkChannel queuedChannel) {
            queuedChannel.drainPacketQueue(channel);
          }

          // sync the locally stored cluster data to the node
          var localNodeServer = this.nodeServerProvider.localNode();
          localNodeServer.updateLocalSnapshot();

          var syncData = this.dataSyncRegistry.prepareClusterData(true, DataSyncHandler::alwaysForceApply);
          channel.sendPacketSync(new ServiceSyncAckPacket(localNodeServer.nodeInfoSnapshot(), syncData));

          // closes the old channel, preventing disconnection handling by setting the state
          // of the channel to 'disconnected' before actually closing the channel
          server.state(NodeServerState.DISCONNECTED);
          server.channel().close();
        } else if (server.channel() instanceof QueuedNetworkChannel queuedChannel) {
          // we reconnected, but the other node either did not notice the disconnect or already removed this node
          // from the cluster. flush the packets that were queued for the node and close the old channel
          queuedChannel.drainPacketQueue(channel);
          queuedChannel.close();
          republishServices = true;
        }

        // re-initialize the node data
        server.channel(channel);
        server.state(NodeServerState.READY);
        channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
        this.networkUtil.addDefaultPacketListeners(channel.packetRegistry());

        // re-select the head node as the node might have been the head node before it disconnected. this
        // ensures that the current node uses the same node as the head node as all other nodes in the cluster
        this.nodeServerProvider.selectHeadNode();

        // the other node might have removed all services of this node already, re-publish them to the node
        if (republishServices) {
          for (var service : this.cloudServiceManager.localCloudServices()) {
            ChannelMessage.builder()
              .targetNode(server.info().uniqueId())
              .message("update_service_info")
              .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
              .build(buffer -> buffer.writeObject(service.serviceInfo()))
              .send();
          }
        }
        return;
      }
    }

    channel.close();
    LOGGER.warn(this.i18n.translate("cluster-server-networking-authorization-failed", channel.serverAddress()));
  }
}
