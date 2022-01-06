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

package eu.cloudnetservice.cloudnet.node.network.listener;

import eu.cloudnetservice.cloudnet.driver.DriverEnvironment;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import eu.cloudnetservice.cloudnet.driver.network.protocol.PacketListener;
import eu.cloudnetservice.cloudnet.node.provider.NodeMessenger;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

public final class PacketServerChannelMessageListener implements PacketListener {

  private final NodeMessenger messenger;
  private final EventManager eventManager;

  public PacketServerChannelMessageListener(@NonNull NodeMessenger messenger, @NonNull EventManager eventManager) {
    this.messenger = messenger;
    this.eventManager = eventManager;
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    var message = packet.content().readObject(ChannelMessage.class);
    // mark the index of the data buf
    message.content().disableReleasing().startTransaction();
    // call the receive event
    var response = this.eventManager.callEvent(
      new ChannelMessageReceiveEvent(message, channel, packet.uniqueId() != null)).queryResponse();
    // reset the index
    message.content().redoTransaction();
    // if the response is already present do not redirect the message to the messenger
    if (response != null) {
      channel.sendPacketSync(packet.constructResponse(DataBuf.empty().writeObject(Collections.singleton(response))));
    } else {
      // do not redirect the channel message to the cluster to prevent infinite loops
      if (packet.uniqueId() != null) {
        var responses = this.messenger
          .sendChannelMessageQueryAsync(message, message.sender().type() == DriverEnvironment.WRAPPER)
          .get(20, TimeUnit.SECONDS, Collections.emptyList());
        // respond with the available responses
        channel.sendPacket(packet.constructResponse(DataBuf.empty().writeObject(responses)));
      } else {
        this.messenger.sendChannelMessage(message, message.sender().type() == DriverEnvironment.WRAPPER);
      }
    }
    // force release the message
    message.content().enableReleasing().release();
  }
}
