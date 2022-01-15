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

package eu.cloudnetservice.cloudnet.node.network;

import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.event.events.network.ChannelType;
import eu.cloudnetservice.cloudnet.driver.event.events.network.NetworkChannelCloseEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.network.def.PacketClientAuthorization;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.network.listener.PacketServerAuthorizationResponseListener;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;

public final class DefaultNetworkClientChannelHandler implements NetworkChannelHandler {

  private static final AtomicLong CONNECTION_COUNTER = new AtomicLong();
  private static final Logger LOGGER = LogManager.logger(DefaultNetworkClientChannelHandler.class);

  @Override
  public void handleChannelInitialize(@NonNull NetworkChannel channel) {
    if (NodeNetworkUtil.shouldInitializeChannel(channel, ChannelType.CLIENT_CHANNEL)) {
      // add the result handler for the auth
      channel.packetRegistry().addListener(
        NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
        new PacketServerAuthorizationResponseListener());
      // send the authentication request
      channel.sendPacket(new PacketClientAuthorization(
        PacketClientAuthorization.PacketAuthorizationType.NODE_TO_NODE,
        DataBuf.empty()
          .writeUniqueId(CloudNet.instance().config().clusterConfig().clusterId())
          .writeObject(CloudNet.instance().config().identity())));

      LOGGER.fine(
        I18n.trans("client-network-channel-init",
          channel.serverAddress(),
          channel.clientAddress().host()));
    } else {
      channel.close();
    }
  }

  @Override
  public boolean handlePacketReceive(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    return !CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelPacketReceiveEvent(channel, packet)).cancelled();
  }

  @Override
  public void handleChannelClose(@NonNull NetworkChannel channel) {
    CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));
    CONNECTION_COUNTER.decrementAndGet();

    LOGGER.fine(
      I18n.trans("client-network-channel-close",
        channel.serverAddress(),
        channel.clientAddress()));

    var clusterNodeServer = CloudNet.instance().nodeServerProvider().nodeServer(channel);
    if (clusterNodeServer != null) {
      NodeNetworkUtil.closeNodeServer(clusterNodeServer);
    }
  }
}
