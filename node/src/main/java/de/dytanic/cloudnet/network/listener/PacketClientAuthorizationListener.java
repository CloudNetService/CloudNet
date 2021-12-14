/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.def.PacketClientAuthorization.PacketAuthorizationType;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.event.network.NetworkClusterNodeAuthSuccessEvent;
import de.dytanic.cloudnet.event.network.NetworkServiceAuthSuccessEvent;
import de.dytanic.cloudnet.network.NodeNetworkUtils;
import de.dytanic.cloudnet.network.packet.PacketServerAuthorizationResponse;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketClientAuthorizationListener implements IPacketListener {

  @Override
  public void handle(@NotNull INetworkChannel channel, @NotNull IPacket packet) {
    // read the core data
    var type = packet.getContent().readObject(PacketAuthorizationType.class);
    try (var content = packet.getContent().readDataBuf()) {
      // handle the authorization
      switch (type) {
        // NODE -> NODE
        case NODE_TO_NODE -> {
          // read the required data for the node auth
          var clusterId = content.readUniqueId();
          var node = content.readObject(NetworkClusterNode.class);
          // check if the cluster id matches
          if (!CloudNet.getInstance().getConfig().getClusterConfig().getClusterId().equals(clusterId)) {
            break;
          }
          // search for the node server which represents the connected node and initialize it
          for (var nodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
            if (nodeServer.isAcceptableConnection(channel, node.getUniqueId())) {
              // set up the node
              nodeServer.setChannel(channel);
              // add the required packet listeners
              channel.getPacketRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
              NodeNetworkUtils.addDefaultPacketListeners(channel.getPacketRegistry(), CloudNet.getInstance());
              // successful auth
              channel.sendPacketSync(new PacketServerAuthorizationResponse(true));
              // call the auth success event
              CloudNet.getInstance().getEventManager().callEvent(
                  new NetworkClusterNodeAuthSuccessEvent(nodeServer, channel));
              // do not search for more nodes
              return;
            }
          }
          break;
        }

        // WRAPPER -> NODE
        case WRAPPER_TO_NODE -> {
          // read the required data for the wrapper auth
          var connectionKey = content.readString();
          var id = content.readObject(ServiceId.class);
          // get the cloud service associated with the service id
          var service = CloudNet.getInstance().getCloudServiceProvider()
              .getLocalCloudService(id.getUniqueId());
          // we can only accept the connection if the service is present, and the connection key is correct
          if (service != null && service.getConnectionKey().equals(connectionKey)) {
            // update the cloud service
            service.setNetworkChannel(channel);
            // send the update to the network
            service.publishServiceInfoSnapshot();
            // add the required packet listeners
            channel.getPacketRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
            NodeNetworkUtils.addDefaultPacketListeners(channel.getPacketRegistry(), CloudNet.getInstance());
            // successful auth
            channel.sendPacket(new PacketServerAuthorizationResponse(true));
            // call the auth success event
            CloudNet.getInstance().getEventManager().callEvent(new NetworkServiceAuthSuccessEvent(service, channel));
            // do not search for other services
            return;
          }
          break;
        }
        default -> {
        }
      }
    }
    // auth not successful
    channel.sendPacketSync(new PacketServerAuthorizationResponse(false));
    channel.close();
  }
}
