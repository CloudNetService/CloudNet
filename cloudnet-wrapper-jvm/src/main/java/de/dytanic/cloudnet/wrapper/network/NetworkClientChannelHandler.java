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

package de.dytanic.cloudnet.wrapper.network;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelCloseEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelInitEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientAuthorization;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;

public class NetworkClientChannelHandler implements INetworkChannelHandler {

  @Override
  public void handleChannelInitialize(@NotNull INetworkChannel channel) {
    NetworkChannelInitEvent networkChannelInitEvent = new NetworkChannelInitEvent(channel, ChannelType.SERVER_CHANNEL);
    CloudNetDriver.getInstance().getEventManager().callEvent(networkChannelInitEvent);

    if (networkChannelInitEvent.isCancelled()) {
      channel.close();
      return;
    }

    networkChannelInitEvent.getChannel().sendPacket(new PacketClientAuthorization(
      PacketClientAuthorization.PacketAuthorizationType.WRAPPER_TO_NODE,
      DataBuf.empty()
        .writeString(Wrapper.getInstance().getConfig().getConnectionKey())
        .writeObject(Wrapper.getInstance().getConfig().getServiceConfiguration().getServiceId())));
  }

  @Override
  public boolean handlePacketReceive(@NotNull INetworkChannel channel, @NotNull Packet packet) {
    if (packet.getUniqueId() != null) {
      CompletableTask<IPacket> waitingHandler = channel.getQueryPacketManager().getWaitingHandler(packet.getUniqueId());
      if (waitingHandler != null) {
        waitingHandler.complete(packet);
      }
    }
    return !CloudNetDriver.getInstance().getEventManager()
      .callEvent(new NetworkChannelPacketReceiveEvent(channel, packet)).isCancelled();
  }

  @Override
  public void handleChannelClose(@NotNull INetworkChannel channel) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));
  }
}
