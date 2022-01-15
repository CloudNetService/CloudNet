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
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.network.listener.PacketClientAuthorizationListener;
import eu.cloudnetservice.cloudnet.node.service.CloudService;
import lombok.NonNull;

public final class DefaultNetworkServerChannelHandler implements NetworkChannelHandler {

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
        new PacketClientAuthorizationListener());

      LOGGER.fine(
        I18n.trans("server-network-channel-init",
          channel.serverAddress(),
          channel.clientAddress()));
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
      new NetworkChannelCloseEvent(channel, ChannelType.SERVER_CHANNEL));

    LOGGER.fine(
      I18n.trans("server-network-channel-close",
        channel.serverAddress(),
        channel.clientAddress()));

    var cloudService = CloudNet.instance()
      .cloudServiceProvider()
      .localCloudServices()
      .stream()
      .filter(service -> service.networkChannel() != null && service.networkChannel().equals(channel))
      .findFirst()
      .orElse(null);
    if (cloudService != null) {
      this.closeAsCloudService(cloudService, channel);
      return;
    }

    var clusterNodeServer = CloudNet.instance().nodeServerProvider().nodeServer(channel);
    if (clusterNodeServer != null) {
      NodeNetworkUtil.closeNodeServer(clusterNodeServer);
    }
  }

  private void closeAsCloudService(@NonNull CloudService cloudService, @NonNull NetworkChannel channel) {
    // reset the service channel and connection time
    cloudService.networkChannel(null);
    cloudService.updateLifecycle(ServiceLifeCycle.STOPPED);

    LOGGER.info(I18n.trans("cloudnet-service-networking-disconnected",
      cloudService.serviceId().uniqueId(),
      cloudService.serviceId().taskName(),
      cloudService.serviceId().name(),
      channel.serverAddress().host() + ":" + channel.serverAddress().port(),
      channel.clientAddress().host() + ":" + channel.clientAddress().port()));
  }

  private boolean shouldDenyConnection(@NonNull NetworkChannel channel) {
    return CloudNet.instance().config().ipWhitelist()
      .stream()
      .noneMatch(channel.clientAddress().host()::equals);
  }
}
