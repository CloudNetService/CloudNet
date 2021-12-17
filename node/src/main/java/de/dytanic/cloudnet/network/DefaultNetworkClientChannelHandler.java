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
import lombok.NonNull;

public final class DefaultNetworkClientChannelHandler implements INetworkChannelHandler {

  private static final AtomicLong CONNECTION_COUNTER = new AtomicLong();
  private static final Logger LOGGER = LogManager.logger(DefaultNetworkClientChannelHandler.class);

  @Override
  public void handleChannelInitialize(@NonNull INetworkChannel channel) {
    if (NodeNetworkUtils.shouldInitializeChannel(channel, ChannelType.CLIENT_CHANNEL)) {
      // add the result handler for the auth
      channel.packetRegistry().addListener(
        NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
        new PacketServerAuthorizationResponseListener());
      // send the authentication request
      channel.sendPacket(new PacketClientAuthorization(
        PacketClientAuthorization.PacketAuthorizationType.NODE_TO_NODE,
        DataBuf.empty()
          .writeUniqueId(CloudNet.instance().getConfig().clusterConfig().clusterId())
          .writeObject(CloudNet.instance().getConfig().identity())));

      LOGGER.fine(I18n.trans("client-network-channel-init")
        .replace("%serverAddress%", channel.serverAddress().host() + ":" + channel.serverAddress().port())
        .replace("%clientAddress%", channel.clientAddress().host() + ":" + channel.clientAddress().port()));
    } else {
      channel.close();
    }
  }

  @Override
  public boolean handlePacketReceive(@NonNull INetworkChannel channel, @NonNull Packet packet) {
    return !CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelPacketReceiveEvent(channel, packet)).cancelled();
  }

  @Override
  public void handleChannelClose(@NonNull INetworkChannel channel) {
    CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));
    CONNECTION_COUNTER.decrementAndGet();

    LOGGER.fine(I18n.trans("client-network-channel-close")
      .replace("%serverAddress%", channel.serverAddress().host() + ":" + channel.serverAddress().port())
      .replace("%clientAddress%", channel.clientAddress().host() + ":" + channel.clientAddress().port()));

    var clusterNodeServer = CloudNet.instance().getClusterNodeServerProvider().nodeServer(channel);
    if (clusterNodeServer != null) {
      NodeNetworkUtils.closeNodeServer(clusterNodeServer);
    }
  }
}
