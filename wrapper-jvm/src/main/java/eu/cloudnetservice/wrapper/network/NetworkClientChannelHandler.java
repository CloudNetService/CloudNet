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

package eu.cloudnetservice.wrapper.network;

import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.event.events.network.ChannelType;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelCloseEvent;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelInitEvent;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.PacketClientAuthorization;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.wrapper.Wrapper;
import lombok.NonNull;

public class NetworkClientChannelHandler implements NetworkChannelHandler {

  @Override
  public void handleChannelInitialize(@NonNull NetworkChannel channel) {
    var networkChannelInitEvent = new NetworkChannelInitEvent(channel, ChannelType.CLIENT_CHANNEL);
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
  public boolean handlePacketReceive(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    return !CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelPacketReceiveEvent(channel, packet)).cancelled();
  }

  @Override
  public void handleChannelClose(@NonNull NetworkChannel channel) {
    CloudNetDriver.instance().eventManager().callEvent(
      new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));
  }
}
