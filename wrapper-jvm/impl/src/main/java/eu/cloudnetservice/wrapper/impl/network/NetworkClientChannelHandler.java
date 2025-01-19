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

package eu.cloudnetservice.wrapper.impl.network;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.network.ChannelType;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelCloseEvent;
import eu.cloudnetservice.driver.event.events.network.NetworkChannelInitEvent;
import eu.cloudnetservice.driver.impl.network.standard.AuthorizationPacket;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkChannelHandler;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import lombok.NonNull;

public final class NetworkClientChannelHandler implements NetworkChannelHandler {

  private final EventManager eventManager;
  private final WrapperConfiguration wrapperConfiguration;

  @Inject
  public NetworkClientChannelHandler(@NonNull EventManager eventManager, @NonNull WrapperConfiguration configuration) {
    this.eventManager = eventManager;
    this.wrapperConfiguration = configuration;
  }

  @Override
  public void handleChannelInitialize(@NonNull NetworkChannel channel) {
    var event = this.eventManager.callEvent(new NetworkChannelInitEvent(channel, ChannelType.CLIENT_CHANNEL));
    if (event.cancelled()) {
      channel.close();
      return;
    }

    channel.sendPacket(new AuthorizationPacket(
      AuthorizationPacket.PacketAuthorizationType.WRAPPER_TO_NODE,
      DataBuf.empty()
        .writeString(this.wrapperConfiguration.connectionKey())
        .writeObject(this.wrapperConfiguration.serviceConfiguration().serviceId())));
  }

  @Override
  public boolean handlePacketReceive(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    return true;
  }

  @Override
  public void handleChannelClose(@NonNull NetworkChannel channel) {
    this.eventManager.callEvent(new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));
  }
}
