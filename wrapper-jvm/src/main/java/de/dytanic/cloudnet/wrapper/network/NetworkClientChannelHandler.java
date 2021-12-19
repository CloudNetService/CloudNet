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

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelCloseEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelInitEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.PacketClientAuthorization;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.wrapper.Wrapper;
import lombok.NonNull;

public class NetworkClientChannelHandler implements INetworkChannelHandler {

  @Override
  public void handleChannelInitialize(@NonNull INetworkChannel channel) {
    var networkChannelInitEvent = new NetworkChannelInitEvent(channel, ChannelType.SERVER_CHANNEL);
    CloudNetDriver.instance().eventManager().callEvent(networkChannelInitEvent);

    if (networkChannelInitEvent.cancelled()) {
      channel.close();
      return;
    }

    networkChannelInitEvent.networkChannel().sendPacket(new PacketClientAuthorization(
      PacketClientAuthorization.PacketAuthorizationType.WRAPPER_TO_NODE,
      DataBuf.empty()
        .writeString(Wrapper.instance().config().connectionKey())
        .writeObject(Wrapper.instance().config().serviceConfiguration().serviceId())));
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
  }
}
