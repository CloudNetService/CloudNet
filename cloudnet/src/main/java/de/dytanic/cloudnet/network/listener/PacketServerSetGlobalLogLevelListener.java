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
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerSetGlobalLogLevel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.service.ICloudService;

public class PacketServerSetGlobalLogLevelListener implements IPacketListener {

  private final boolean redirectToCluster;

  public PacketServerSetGlobalLogLevelListener(boolean redirectToCluster) {
    this.redirectToCluster = redirectToCluster;
  }

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    int level = packet.getBuffer().readInt();
    packet = new PacketServerSetGlobalLogLevel(level);

    CloudNetDriver.getInstance().getLogger().setLevel(level);

    for (ICloudService localCloudService : CloudNet.getInstance().getCloudServiceManager().getLocalCloudServices()) {
      if (localCloudService.getNetworkChannel() != null) {
        localCloudService.getNetworkChannel().sendPacket(packet);
      }
    }
    if (this.redirectToCluster) {
      for (IClusterNodeServer nodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
        if (nodeServer.getChannel() != null) {
          nodeServer.getChannel().sendPacket(packet);
        }
      }
    }
  }

}
