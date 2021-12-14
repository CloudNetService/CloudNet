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
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelCloseEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.def.PacketClientAuthorization;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.network.listener.PacketServerAuthorizationResponseListener;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

public final class DefaultNetworkClientChannelHandler implements INetworkChannelHandler {

  private static final AtomicLong CONNECTION_COUNTER = new AtomicLong();
  private static final Logger LOGGER = LogManager.getLogger(DefaultNetworkClientChannelHandler.class);

  @Override
  public void handleChannelInitialize(@NotNull INetworkChannel channel) {
    if (NodeNetworkUtils.shouldInitializeChannel(channel, ChannelType.CLIENT_CHANNEL)) {
      // add the result handler for the auth
      channel.getPacketRegistry().addListener(
        NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
        new PacketServerAuthorizationResponseListener());
      // send the authentication request
      channel.sendPacket(new PacketClientAuthorization(
        PacketClientAuthorization.PacketAuthorizationType.NODE_TO_NODE,
        DataBuf.empty()
          .writeUniqueId(CloudNet.getInstance().getConfig().getClusterConfig().getClusterId())
          .writeObject(CloudNet.getInstance().getConfig().getIdentity())));

      LOGGER.fine(I18n.trans("client-network-channel-init")
        .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
        .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort()));
    } else {
      channel.close();
    }
  }

  @Override
  public boolean handlePacketReceive(@NotNull INetworkChannel channel, @NotNull Packet packet) {
    return !CloudNetDriver.getInstance().getEventManager().callEvent(
      new NetworkChannelPacketReceiveEvent(channel, packet)).isCancelled();
  }

  @Override
  public void handleChannelClose(@NotNull INetworkChannel channel) {
    CloudNetDriver.getInstance().getEventManager().callEvent(
      new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));
    CONNECTION_COUNTER.decrementAndGet();

    LOGGER.fine(I18n.trans("client-network-channel-close")
      .replace("%serverAddress%", channel.getServerAddress().getHost() + ":" + channel.getServerAddress().getPort())
      .replace("%clientAddress%", channel.getClientAddress().getHost() + ":" + channel.getClientAddress().getPort()));

    var clusterNodeServer = CloudNet.getInstance().getClusterNodeServerProvider().getNodeServer(channel);
    if (clusterNodeServer != null) {
      NodeNetworkUtils.closeNodeServer(clusterNodeServer);
    }
  }
}
