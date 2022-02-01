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

package eu.cloudnetservice.cloudnet.node.network.listener;

import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNode;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.network.def.PacketClientAuthorization.PacketAuthorizationType;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import eu.cloudnetservice.cloudnet.driver.network.protocol.PacketListener;
import eu.cloudnetservice.cloudnet.driver.service.ServiceId;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServerState;
import eu.cloudnetservice.cloudnet.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.cloudnet.node.event.network.NetworkClusterNodeAuthSuccessEvent;
import eu.cloudnetservice.cloudnet.node.event.network.NetworkClusterNodeReconnectEvent;
import eu.cloudnetservice.cloudnet.node.event.network.NetworkServiceAuthSuccessEvent;
import eu.cloudnetservice.cloudnet.node.network.NodeNetworkUtil;
import eu.cloudnetservice.cloudnet.node.network.packet.PacketServerAuthorizationResponse;
import lombok.NonNull;

public final class PacketClientAuthorizationListener implements PacketListener {

  private static final Logger LOGGER = LogManager.logger(PacketServerAuthorizationResponseListener.class);

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // read the core data
    var type = packet.content().readObject(PacketAuthorizationType.class);
    try (var content = packet.content().readDataBuf()) {
      // handle the authorization
      switch (type) {
        // NODE -> NODE
        case NODE_TO_NODE -> {
          // read the required data for the node auth
          var clusterId = content.readUniqueId();
          var node = content.readObject(NetworkClusterNode.class);
          // check if the cluster id matches
          if (!CloudNet.instance().config().clusterConfig().clusterId().equals(clusterId)) {
            break;
          }
          // search for the node server which represents the connected node and initialize it
          for (var server : CloudNet.instance().nodeServerProvider().nodeServers()) {
            if (server.info().uniqueId().equals(node.uniqueId())) {
              // add the required packet listeners
              NodeNetworkUtil.addDefaultPacketListeners(channel.packetRegistry(), CloudNet.instance());
              channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
              // check if the node is currently marked disconnected and reconnected to the network
              if (server.state() == NodeServerState.DISCONNECTED) {
                // respond with an auth success
                var data = CloudNet.instance().dataSyncRegistry().prepareClusterData(
                  true,
                  DataSyncHandler::alwaysForceApply);
                channel.sendPacket(new PacketServerAuthorizationResponse(true, true, data));
                // reset the state of the server
                server.state(NodeServerState.SYNCING);
                // call the node reconnect success event
                CloudNet.instance().eventManager().callEvent(new NetworkClusterNodeReconnectEvent(server, channel));
              } else {
                // reply with a default auth success
                channel.sendPacket(new PacketServerAuthorizationResponse(true, false, null));
                // set the state of the node for further handling
                server.channel(channel);
                server.state(NodeServerState.READY);
                // call the auth success event
                CloudNet.instance().eventManager().callEvent(new NetworkClusterNodeAuthSuccessEvent(server, channel));
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
          var service = CloudNet.instance().cloudServiceProvider()
            .localCloudService(id.uniqueId());
          // we can only accept the connection if the service is present, and the connection key is correct
          if (service != null && service.connectionKey().equals(connectionKey)) {
            // update the cloud service
            service.networkChannel(channel);
            // send the update to the network
            service.publishServiceInfoSnapshot();
            // add the required packet listeners
            channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
            NodeNetworkUtil.addDefaultPacketListeners(channel.packetRegistry(), CloudNet.instance());
            // successful auth
            channel.sendPacket(new PacketServerAuthorizationResponse(true, false, null));
            // call the auth success event
            CloudNet.instance().eventManager().callEvent(new NetworkServiceAuthSuccessEvent(service, channel));
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
