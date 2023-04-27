/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.network;

import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.network.ChannelType;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelCloseEvent;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.def.PacketClientAuthorization;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.network.listener.PacketServerAuthorizationResponseListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;

@Singleton
public final class DefaultNetworkClientChannelHandler implements NetworkChannelHandler {

  private static final AtomicLong CONNECTION_COUNTER = new AtomicLong();
  private static final Logger LOGGER = LogManager.logger(DefaultNetworkClientChannelHandler.class);

  private final EventManager eventManager;
  private final NodeNetworkUtil networkUtil;
  private final Configuration configuration;
  private final NodeServerProvider nodeServerProvider;

  @Inject
  public DefaultNetworkClientChannelHandler(
    @NonNull EventManager eventManager,
    @NonNull NodeNetworkUtil networkUtil,
    @NonNull Configuration configuration,
    @NonNull NodeServerProvider nodeServerProvider
  ) {
    this.eventManager = eventManager;
    this.networkUtil = networkUtil;
    this.configuration = configuration;
    this.nodeServerProvider = nodeServerProvider;
  }

  @Override
  public void handleChannelInitialize(@NonNull NetworkChannel channel) {
    if (this.networkUtil.shouldInitializeChannel(channel, ChannelType.CLIENT_CHANNEL)) {
      // add the result handler for the auth
      channel.packetRegistry().addListener(
        NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
        PacketServerAuthorizationResponseListener.class);
      // send the authentication request
      channel.sendPacket(new PacketClientAuthorization(
        PacketClientAuthorization.PacketAuthorizationType.NODE_TO_NODE,
        DataBuf.empty()
          .writeUniqueId(this.configuration.clusterConfig().clusterId())
          .writeObject(this.configuration.identity())));

      LOGGER.fine(I18n.trans("client-network-channel-init",
        channel.serverAddress(),
        channel.clientAddress()));
    } else {
      channel.close();
    }
  }

  @Override
  public boolean handlePacketReceive(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    return !this.eventManager.callEvent(new NetworkChannelPacketReceiveEvent(channel, packet)).cancelled();
  }

  @Override
  public void handleChannelClose(@NonNull NetworkChannel channel) {
    CONNECTION_COUNTER.decrementAndGet();
    this.eventManager.callEvent(new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));

    LOGGER.fine(I18n.trans("client-network-channel-close",
      channel.serverAddress(),
      channel.clientAddress()));

    var clusterNodeServer = this.nodeServerProvider.node(channel);
    if (clusterNodeServer != null && clusterNodeServer.state() != NodeServerState.DISCONNECTED) {
      clusterNodeServer.close();
    }
  }
}
