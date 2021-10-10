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
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.network.packet.PacketServerAuthorizationResponse;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketClientAuthorizationListener implements IPacketListener {

  @Override
  public void handle(@NotNull INetworkChannel channel, IPacket packet) throws Exception {
    // read the core data
    int type = packet.getContent().readInt();
    DataBuf content = packet.getContent().readDataBuf();
    // handle the authorization
    switch (type) {
      // NODE -> NODE
      case 0: {
        // read the required data for the node auth
        UUID clusterId = content.readUniqueId();
        NetworkClusterNode node = content.readObject(NetworkClusterNode.class);
        // check if the cluster id matches
        if (!CloudNet.getInstance().getConfig().getClusterConfig().getClusterId().equals(clusterId)) {
          break;
        }
        // search for the node server which represents the connected node and initialize it
        for (IClusterNodeServer nodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
          if (nodeServer.isAcceptableConnection(channel, node.getUniqueId())) {
            // set up the node
            nodeServer.setChannel(channel);
            // successful auth
            channel.sendPacket(new PacketServerAuthorizationResponse(true));
            ChannelMessage.builder()
              .targetNode(node.getUniqueId())
              .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
              .message("initial_service_list_information")
              .buffer(DataBuf.empty().writeObject(CloudNet.getInstance().getCloudServiceProvider().getCloudServices()))
              .build()
              .send();
            // do not search for more nodes
            return;
          }
        }
        break;
      }
      // WRAPPER -> NODE
      case 1: {
        // read the required data for the wrapper auth
        String connectionKey = content.readString();
        ServiceId id = content.readObject(ServiceId.class);
        // get the cloud service associated with the service id
        ICloudService service = CloudNet.getInstance().getCloudServiceProvider().getLocalCloudService(id.getUniqueId());
        // we can only accept the connection if the service is present, and the connection key is correct
        if (service != null && service.getConnectionKey().equals(connectionKey)) {
          // update the cloud service
          service.setNetworkChannel(channel);
          // send the update to the network
          service.publishServiceInfoSnapshot();
          // successful auth
          channel.sendPacket(new PacketServerAuthorizationResponse(true));
          return;
        }
        break;
      }
      default:
        break;
    }
    // auth not successful
    channel.sendPacketSync(new PacketServerAuthorizationResponse(false));
    channel.close();
  }
}
