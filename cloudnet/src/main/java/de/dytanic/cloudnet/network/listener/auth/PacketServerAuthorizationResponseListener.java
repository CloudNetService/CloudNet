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

package de.dytanic.cloudnet.network.listener.auth;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.network.ClusterUtils;

public final class PacketServerAuthorizationResponseListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    if (packet.getHeader().contains("access")) {
      if (packet.getHeader().getBoolean("access")) {
        for (NetworkClusterNode node : CloudNet.getInstance().getConfig().getClusterConfig().getNodes()) {
          for (HostAndPort hostAndPort : node.getListeners()) {
            if (hostAndPort.getPort() == channel.getServerAddress().getPort() &&
              hostAndPort.getHost().equals(channel.getServerAddress().getHost())) {

              IClusterNodeServer nodeServer = CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()
                .stream()
                .filter(clusterNodeServer -> clusterNodeServer.getNodeInfo().getUniqueId().equals(node.getUniqueId()))
                .findFirst().orElse(null);

              if (nodeServer != null && nodeServer.isAcceptableConnection(channel, node.getUniqueId())) {
                nodeServer.setChannel(channel);
                ClusterUtils.sendSetupInformationPackets(channel);

                CloudNetDriver.getInstance().getEventManager()
                  .callEvent(new NetworkChannelAuthClusterNodeSuccessEvent(nodeServer, channel));

                CloudNet.getInstance().getLogger().info(
                  LanguageManager.getMessage("cluster-server-networking-connected")
                    .replace("%id%", node.getUniqueId())
                    .replace("%serverAddress%",
                      channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
                    .replace("%clientAddress%",
                      channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
                );
                break;
              }
            }
          }
        }
      } else {
        CloudNet.getInstance().getLogger()
          .log(LogLevel.WARNING, LanguageManager.getMessage("cluster-server-networking-authorization-failed"));
      }
    }
  }
}
