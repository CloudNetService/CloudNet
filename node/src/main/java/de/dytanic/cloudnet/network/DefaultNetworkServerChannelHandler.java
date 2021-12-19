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
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.network.listener.PacketClientAuthorizationListener;
import de.dytanic.cloudnet.service.ICloudService;
import lombok.NonNull;

public final class DefaultNetworkServerChannelHandler implements INetworkChannelHandler {

  private static final Logger LOGGER = LogManager.logger(DefaultNetworkServerChannelHandler.class);

  @Override
  public void handleChannelInitialize(@NonNull INetworkChannel channel) {
    // check if the ip of the connecting client is allowed
    if (this.shouldDenyConnection(channel)) {
      channel.close();
      return;
    }

    if (NodeNetworkUtils.shouldInitializeChannel(channel, ChannelType.SERVER_CHANNEL)) {
      // add the auth listener
      channel.packetRegistry().addListener(
        NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
        new PacketClientAuthorizationListener());

      LOGGER.fine(I18n.trans("server-network-channel-init")
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
      new NetworkChannelCloseEvent(channel, ChannelType.SERVER_CHANNEL));

    LOGGER.fine(I18n.trans("server-network-channel-close")
      .replace("%serverAddress%", channel.serverAddress().host() + ":" + channel.serverAddress().port())
      .replace("%clientAddress%", channel.clientAddress().host() + ":" + channel.clientAddress().port()));

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
      NodeNetworkUtils.closeNodeServer(clusterNodeServer);
    }
  }

  private void closeAsCloudService(@NonNull ICloudService cloudService, @NonNull INetworkChannel channel) {
    // reset the service channel and connection time
    cloudService.networkChannel(null);
    cloudService.updateLifecycle(ServiceLifeCycle.STOPPED);

    LOGGER.info(I18n.trans("cloud-service-networking-disconnected")
      .replace("%id%", cloudService.serviceId().uniqueId().toString())
      .replace("%task%", cloudService.serviceId().taskName())
      .replace("%serviceId%", String.valueOf(cloudService.serviceId().taskServiceId()))
      .replace("%serverAddress%", channel.serverAddress().host() + ":" + channel.serverAddress().port())
      .replace("%clientAddress%", channel.clientAddress().host() + ":" + channel.clientAddress().port()));
  }

  private boolean shouldDenyConnection(@NonNull INetworkChannel channel) {
    return CloudNet.instance().config().ipWhitelist()
      .stream()
      .noneMatch(channel.clientAddress().host()::equals);
  }
}
