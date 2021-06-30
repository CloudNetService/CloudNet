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

package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public final class PacketServerChannelMessageListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    ChannelMessage message = packet.getBuffer().readObject(ChannelMessage.class);
    boolean query = packet.getBuffer().readBoolean();

    ChannelMessageReceiveEvent event = new ChannelMessageReceiveEvent(message, query);
    CloudNetDriver.getInstance().getEventManager().callEvent(event);

    if (query) {
      ProtocolBuffer buffer = ProtocolBuffer.create();
      buffer.writeBoolean(event.getQueryResponse() != null);
      if (event.getQueryResponse() != null) {
        buffer.writeObject(event.getQueryResponse());
      }
      channel.sendPacket(new Packet(-1, packet.getUniqueId(), JsonDocument.EMPTY, buffer));
    }
  }
}
