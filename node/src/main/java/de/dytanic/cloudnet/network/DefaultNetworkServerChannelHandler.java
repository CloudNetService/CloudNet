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
import org.jetbrains.annotations.NotNull;

public final class DefaultNetworkServerChannelHandler implements INetworkChannelHandler {

  private static final Logger LOGGER = LogManager.logger(DefaultNetworkServerChannelHandler.class);

  @Override
  public void handleChannelInitialize(@NotNull INetworkChannel channel) {
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
  public boolean handlePacketReceive(@NotNull INetworkChannel channel, @NotNull Packet packet) {
    return !CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelPacketReceiveEvent(channel, packet)).cancelled();
  }

  @Override
  public void handleChannelClose(@NotNull INetworkChannel channel) {
    CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelCloseEvent(channel, ChannelType.SERVER_CHANNEL));

    LOGGER.fine(I18n.trans("server-network-channel-close")
      .replace("%serverAddress%", channel.serverAddress().host() + ":" + channel.serverAddress().port())
      .replace("%clientAddress%", channel.clientAddress().host() + ":" + channel.clientAddress().port()));

    var cloudService = CloudNet.getInstance()
      .cloudServiceProvider()
      .getLocalCloudServices()
      .stream()
      .filter(service -> service.getNetworkChannel() != null && service.getNetworkChannel().equals(channel))
      .findFirst()
      .orElse(null);
    if (cloudService != null) {
      this.closeAsCloudService(cloudService, channel);
      return;
    }

    var clusterNodeServer = CloudNet.getInstance().getClusterNodeServerProvider().getNodeServer(channel);
    if (clusterNodeServer != null) {
      NodeNetworkUtils.closeNodeServer(clusterNodeServer);
    }
  }

  private void closeAsCloudService(@NotNull ICloudService cloudService, @NotNull INetworkChannel channel) {
    // reset the service channel and connection time
    cloudService.setNetworkChannel(null);
    cloudService.updateLifecycle(ServiceLifeCycle.STOPPED);

    LOGGER.info(I18n.trans("cloud-service-networking-disconnected")
      .replace("%id%", cloudService.getServiceId().uniqueId().toString())
      .replace("%task%", cloudService.getServiceId().taskName())
      .replace("%serviceId%", String.valueOf(cloudService.getServiceId().taskServiceId()))
      .replace("%serverAddress%", channel.serverAddress().host() + ":" + channel.serverAddress().port())
      .replace("%clientAddress%", channel.clientAddress().host() + ":" + channel.clientAddress().port()));
  }

  private boolean shouldDenyConnection(@NotNull INetworkChannel channel) {
    return CloudNet.getInstance().getConfig().getIpWhitelist()
      .stream()
      .noneMatch(channel.clientAddress().host()::equals);
  }
}
