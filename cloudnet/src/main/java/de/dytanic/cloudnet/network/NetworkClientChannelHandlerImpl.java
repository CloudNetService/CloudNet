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

package de.dytanic.cloudnet.network;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelCloseEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.def.internal.InternalSyncPacketChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientAuthorization;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import java.util.concurrent.atomic.AtomicLong;

public final class NetworkClientChannelHandlerImpl implements INetworkChannelHandler {

  private static final AtomicLong connectionWhichSendRequest = new AtomicLong();

  @Override
  public void handleChannelInitialize(INetworkChannel channel) {
    if (!NetworkChannelHandlerUtils.handleInitChannel(channel, ChannelType.CLIENT_CHANNEL)) {
      return;
    }

    channel.sendPacket(new PacketClientAuthorization(
      PacketClientAuthorization.PacketAuthorizationType.NODE_TO_NODE,
      new JsonDocument("clusterNode", CloudNet.getInstance().getConfig().getIdentity())
        .append("clusterId", CloudNet.getInstance().getConfig().getClusterConfig().getClusterId())
        .append("secondNodeConnection", connectionWhichSendRequest.incrementAndGet() > 1)
    ));

    CloudNetDriver.optionalInstance().ifPresent(
      cloudNetDriver -> cloudNetDriver.getLogger().extended(LanguageManager.getMessage("client-network-channel-init")
        .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
        .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
      ));
  }

  @Override
  public boolean handlePacketReceive(INetworkChannel channel, Packet packet) {
    if (InternalSyncPacketChannel.handleIncomingChannel(channel, packet)) {
      return false;
    }

    return !CloudNetDriver.getInstance().getEventManager()
      .callEvent(new NetworkChannelPacketReceiveEvent(channel, packet)).isCancelled();
  }

  @Override
  public void handleChannelClose(INetworkChannel channel) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));
    connectionWhichSendRequest.decrementAndGet();

    CloudNetDriver.optionalInstance().ifPresent(
      cloudNetDriver -> cloudNetDriver.getLogger().extended(LanguageManager.getMessage("client-network-channel-close")
        .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
        .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort())
      ));

    IClusterNodeServer clusterNodeServer = CloudNet.getInstance().getClusterNodeServerProvider().getNodeServer(channel);

    if (clusterNodeServer != null) {
      NetworkChannelHandlerUtils.closeNodeServer(clusterNodeServer);
    }
  }
}
