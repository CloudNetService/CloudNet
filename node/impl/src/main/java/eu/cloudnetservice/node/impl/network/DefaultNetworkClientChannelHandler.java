/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.network;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.network.ChannelType;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelCloseEvent;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.impl.network.standard.AuthorizationPacket;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.network.listener.AuthorizationResponsePacketListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DefaultNetworkClientChannelHandler implements NetworkChannelHandler {

  private static final AtomicLong CONNECTION_COUNTER = new AtomicLong();
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNetworkClientChannelHandler.class);

  private final I18n i18n;
  private final EventManager eventManager;
  private final NodeNetworkUtil networkUtil;
  private final Configuration configuration;
  private final NodeServerProvider nodeServerProvider;

  @Inject
  public DefaultNetworkClientChannelHandler(
    @NonNull @Service I18n i18n,
    @NonNull EventManager eventManager,
    @NonNull NodeNetworkUtil networkUtil,
    @NonNull Configuration configuration,
    @NonNull NodeServerProvider nodeServerProvider
  ) {
    this.i18n = i18n;
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
        AuthorizationResponsePacketListener.class);
      // send the authentication request
      channel.sendPacket(new AuthorizationPacket(
        AuthorizationPacket.PacketAuthorizationType.NODE_TO_NODE,
        DataBuf.empty()
          .writeUniqueId(this.configuration.clusterConfig().clusterId())
          .writeObject(this.configuration.identity())));

      LOGGER.debug(this.i18n.translate("client-network-channel-init",
        channel.serverAddress(),
        channel.clientAddress().host()));
    } else {
      channel.close();
    }
  }

  @Override
  public boolean handlePacketReceive(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    return true;
  }

  @Override
  public void handleChannelClose(@NonNull NetworkChannel channel) {
    CONNECTION_COUNTER.decrementAndGet();
    this.eventManager.callEvent(new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));

    LOGGER.debug(this.i18n.translate("client-network-channel-close",
      channel.serverAddress(),
      channel.clientAddress()));

    var clusterNodeServer = this.nodeServerProvider.node(channel);
    if (clusterNodeServer != null && clusterNodeServer.state() != NodeServerState.DISCONNECTED) {
      clusterNodeServer.close();
    }
  }
}
