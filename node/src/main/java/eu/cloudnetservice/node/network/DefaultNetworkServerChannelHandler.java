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

package eu.cloudnetservice.node.network;

import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.events.network.ChannelType;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelCloseEvent;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.network.listener.PacketClientAuthorizationListener;
import eu.cloudnetservice.node.service.CloudService;
import lombok.NonNull;

public record DefaultNetworkServerChannelHandler(@NonNull Node node) implements NetworkChannelHandler {

  private static final Logger LOGGER = LogManager.logger(DefaultNetworkServerChannelHandler.class);

  @Override
  public void handleChannelInitialize(@NonNull NetworkChannel channel) {
    // check if the ip of the connecting client is allowed
    if (this.shouldDenyConnection(channel)) {
      channel.close();
      return;
    }

    if (NodeNetworkUtil.shouldInitializeChannel(channel, ChannelType.SERVER_CHANNEL)) {
      // add the auth listener
      channel.packetRegistry().addListener(
        NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
        new PacketClientAuthorizationListener(this.node));

      LOGGER.fine(I18n.trans("server-network-channel-init",
        channel.serverAddress(),
        channel.clientAddress()));
    } else {
      channel.close();
    }
  }

  @Override
  public boolean handlePacketReceive(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    return !this.node.eventManager().callEvent(
      new NetworkChannelPacketReceiveEvent(channel, packet)).cancelled();
  }

  @Override
  public void handleChannelClose(@NonNull NetworkChannel channel) {
    this.node.eventManager().callEvent(
      new NetworkChannelCloseEvent(channel, ChannelType.SERVER_CHANNEL));

    LOGGER.fine(I18n.trans("server-network-channel-close",
      channel.serverAddress(),
      channel.clientAddress()));

    var cloudService = this.node
      .cloudServiceProvider()
      .localCloudServices()
      .stream()
      .filter(service -> {
        var serviceNetworkChannel = service.networkChannel();
        return serviceNetworkChannel != null && serviceNetworkChannel.equals(channel);
      })
      .findFirst()
      .orElse(null);
    if (cloudService != null) {
      this.closeAsCloudService(cloudService, channel);
      return;
    }

    var nodeServer = this.node.nodeServerProvider().node(channel);
    if (nodeServer != null && nodeServer.state() != NodeServerState.DISCONNECTED) {
      nodeServer.close();
    }
  }

  private void closeAsCloudService(@NonNull CloudService cloudService, @NonNull NetworkChannel channel) {
    // reset the service channel and connection time
    cloudService.networkChannel(null);

    LOGGER.info(I18n.trans("cloudnet-service-networking-disconnected",
      cloudService.serviceId().uniqueId(),
      cloudService.serviceId().taskName(),
      cloudService.serviceId().name(),
      channel.serverAddress().host() + ":" + channel.serverAddress().port(),
      channel.clientAddress().host() + ":" + channel.clientAddress().port()));
  }

  private boolean shouldDenyConnection(@NonNull NetworkChannel channel) {
    return this.node.config().ipWhitelist()
      .stream()
      .noneMatch(channel.clientAddress().host()::equals);
  }
}
