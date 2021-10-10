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

package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.provider.NodeMessenger;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class PacketServerChannelMessageListener implements IPacketListener {

  private final NodeMessenger messenger;
  private final IEventManager eventManager;

  public PacketServerChannelMessageListener(NodeMessenger messenger, IEventManager eventManager) {
    this.messenger = messenger;
    this.eventManager = eventManager;
  }

  @Override
  public void handle(@NotNull INetworkChannel channel, @NotNull IPacket packet) throws Exception {
    ChannelMessage message = packet.getContent().readObject(ChannelMessage.class);
    // call the receive event
    ChannelMessage response = this.eventManager.callEvent(
      new ChannelMessageReceiveEvent(message, packet.getUniqueId() != null)).getQueryResponse();
    // if the response is already present do not redirect the message to the messenger
    if (response != null) {
      channel.sendPacket(packet.constructResponse(DataBuf.empty().writeObject(response)));
    } else {
      // do not redirect the channel message to the cluster to prevent infinite loops
      if (packet.getUniqueId() != null) {
        Collection<ChannelMessage> responses = this.messenger
          .sendChannelMessageQueryAsync(message, false)
          .get(20, TimeUnit.SECONDS, Collections.emptyList());
        // respond with the available responses
        channel.sendPacket(packet.constructResponse(DataBuf.empty().writeObject(responses)));
      } else {
        this.messenger.sendChannelMessage(message, false);
      }
    }
  }
}
