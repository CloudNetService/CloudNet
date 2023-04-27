/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.def.PacketClientAuthorization;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.event.network.NetworkClusterNodeAuthSuccessEvent;
import eu.cloudnetservice.node.event.network.NetworkClusterNodeReconnectEvent;
import eu.cloudnetservice.node.event.network.NetworkServiceAuthSuccessEvent;
import eu.cloudnetservice.node.network.NodeNetworkUtil;
import eu.cloudnetservice.node.network.packet.PacketServerAuthorizationResponse;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class PacketClientAuthorizationListener implements PacketListener {

  private static final Logger LOGGER = LogManager.logger(PacketServerAuthorizationResponseListener.class);

  private final EventManager eventManager;
  private final Configuration configuration;
  private final NodeNetworkUtil networkUtil;
  private final DataSyncRegistry dataSyncRegistry;
  private final NodeServerProvider nodeServerProvider;
  private final CloudServiceManager cloudServiceManager;

  @Inject
  public PacketClientAuthorizationListener(
    @NonNull EventManager eventManager,
    @NonNull Configuration configuration,
    @NonNull NodeNetworkUtil networkUtil,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull CloudServiceManager cloudServiceManager
  ) {
    this.eventManager = eventManager;
    this.configuration = configuration;
    this.networkUtil = networkUtil;
    this.dataSyncRegistry = dataSyncRegistry;
    this.nodeServerProvider = nodeServerProvider;
    this.cloudServiceManager = cloudServiceManager;
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // read the core data
    var type = packet.content().readObject(PacketClientAuthorization.PacketAuthorizationType.class);
    try (var content = packet.content().readDataBuf()) {
      // handle the authorization
      switch (type) {
        // NODE -> NODE
        case NODE_TO_NODE -> {
          // read the required data for the node auth
          var clusterId = content.readUniqueId();
          var node = content.readObject(NetworkClusterNode.class);
          // check if the cluster id matches
          if (!this.configuration.clusterConfig().clusterId().equals(clusterId)) {
            break;
          }
          // search for the node server which represents the connected node and initialize it
          for (var server : this.nodeServerProvider.nodeServers()) {
            if (server.info().uniqueId().equals(node.uniqueId())) {
              // add the required packet listeners
              this.networkUtil.addDefaultPacketListeners(channel.packetRegistry());
              channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
              // check if the node is currently marked disconnected and reconnected to the network
              if (server.state() == NodeServerState.DISCONNECTED) {
                // respond with an auth success
                var data = this.dataSyncRegistry.prepareClusterData(true, DataSyncHandler::alwaysForceApply);
                channel.sendPacket(new PacketServerAuthorizationResponse(true, true, data));
                channel.packetRegistry().addListener(
                  NetworkConstants.INTERNAL_SERVICE_SYNC_ACK_CHANNEL,
                  PacketClientServiceSyncAckListener.class);
                // reset the state of the server
                server.state(NodeServerState.SYNCING);
                // call the node reconnect success event
                this.eventManager.callEvent(new NetworkClusterNodeReconnectEvent(server, channel));
              } else {
                // reply with a default auth success
                channel.sendPacket(new PacketServerAuthorizationResponse(true, false, null));
                // set the state of the node for further handling
                server.channel(channel);
                server.state(NodeServerState.READY);
                // call the auth success event
                this.eventManager.callEvent(new NetworkClusterNodeAuthSuccessEvent(server, channel));
              }

              // do not search for more nodes
              return;
            }
          }
        }

        // WRAPPER -> NODE
        case WRAPPER_TO_NODE -> {
          // read the required data for the wrapper auth
          var connectionKey = content.readString();
          var id = content.readObject(ServiceId.class);
          // get the cloud service associated with the service id
          var service = this.cloudServiceManager.localCloudService(id.uniqueId());
          // we can only accept the connection if the service is present, and the connection key is correct
          if (service != null && service.connectionKey().equals(connectionKey)) {
            // update the cloud service
            service.networkChannel(channel);
            // send the update to the network
            service.publishServiceInfoSnapshot();
            // add the required packet listeners
            channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
            this.networkUtil.addDefaultPacketListeners(channel.packetRegistry());
            // successful auth
            channel.sendPacket(new PacketServerAuthorizationResponse(true, false, null));
            // call the auth success event
            this.eventManager.callEvent(new NetworkServiceAuthSuccessEvent(service, channel));
            var serviceId = service.serviceId();
            LOGGER.info(I18n.trans("cloudnet-service-networking-connected",
              serviceId.uniqueId(),
              serviceId.taskName(),
              serviceId.name(),
              channel.serverAddress(),
              channel.clientAddress()));
            // do not search for other services
            return;
          }
        }
        default -> {
        }
      }
    }
    // auth not successful
    channel.sendPacketSync(new PacketServerAuthorizationResponse(false, false, null));
    channel.close();
  }
}
