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

package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.ArrayList;
import java.util.Collection;

final class ClusterNodeServerUtils {

  private static final Logger LOGGER = LogManager.getLogger(ClusterNodeServerUtils.class);

  private ClusterNodeServerUtils() {
    throw new UnsupportedOperationException();
  }

  public static void handleNodeServerClose(INetworkChannel channel, IClusterNodeServer server) {
    Collection<Packet> removed = new ArrayList<>();

    for (ServiceInfoSnapshot snapshot : CloudNet.getInstance().getCloudServiceProvider().getCloudServices()) {
      if (snapshot.getServiceId().getNodeUniqueId().equalsIgnoreCase(server.getNodeInfo().getUniqueId())) {
        // mark the service as deleted
        snapshot.setLifeCycle(ServiceLifeCycle.DELETED);
        // publish the update to the manager and to the network
        CloudNet.getInstance().getCloudServiceProvider().handleServiceUpdate(snapshot);
      }
    }
/* TODO
    for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceProvider().getCloudServices().values()) {
      if (cloudService.getNetworkChannel() != null) {
        for (Packet packet : removed) {
          cloudService.getNetworkChannel().sendPacket(packet);
        }
      }
    }
*/
    LOGGER.info(LanguageManager.getMessage("cluster-server-networking-disconnected")
      .replace("%id%", server.getNodeInfo().getUniqueId())
      .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
      .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
    );
  }
}
