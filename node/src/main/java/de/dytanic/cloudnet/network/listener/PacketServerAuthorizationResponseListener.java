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

package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import de.dytanic.cloudnet.network.NodeNetworkUtils;
import java.util.Arrays;
import java.util.Objects;
import lombok.NonNull;

public final class PacketServerAuthorizationResponseListener implements PacketListener {

  private static final Logger LOGGER = LogManager.logger(PacketServerAuthorizationResponseListener.class);

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // check if the auth was successful
    if (packet.content().readBoolean()) {
      // search for the node to which the auth succeeded
      var server = CloudNet.instance().config().clusterConfig().nodes().stream()
        .filter(node -> Arrays.stream(node.listeners()).anyMatch(host -> channel.serverAddress().equals(host)))
        .map(node -> CloudNet.instance().nodeServerProvider().nodeServer(node.uniqueId()))
        .filter(Objects::nonNull)
        .filter(node -> node.acceptableConnection(channel, node.nodeInfo().uniqueId()))
        .findFirst()
        .orElse(null);
      if (server != null) {
        server.channel(channel);
        // add the packet listeners
        channel.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);
        NodeNetworkUtils.addDefaultPacketListeners(channel.packetRegistry(), CloudNet.instance());
        // we are good to go :)
        return;
      }
    }

    channel.close();
    LOGGER.warning(I18n.trans("cluster-server-networking-authorization-failed"));
  }
}
