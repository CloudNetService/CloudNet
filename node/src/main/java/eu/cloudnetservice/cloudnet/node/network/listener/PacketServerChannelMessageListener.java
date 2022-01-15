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

import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import eu.cloudnetservice.cloudnet.driver.network.protocol.PacketListener;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.provider.NodeMessenger;
import java.util.ArrayList;
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
    var responses = new ArrayList<>();
    var comesFromWrapper = packet.content().readBoolean();
    var message = packet.content().readObject(ChannelMessage.class);
    // disable releasing of the message content
    message.content().disableReleasing();
    // check if we should handle the message locally
    var handleLocally = message.targets().stream().anyMatch(target -> switch (target.type()) {
      case ALL -> true;
      case NODE -> target.name() == null || target.name().equals(CloudNet.instance().componentName());
      default -> false;
    });
    if (handleLocally) {
      // mark the index of the data buf
      message.content().startTransaction();
      // call the receive event
      var response = this.eventManager.callEvent(
        new ChannelMessageReceiveEvent(message, channel, packet.uniqueId() != null)).queryResponse();
      // reset the index
      message.content().redoTransaction();
      // add the response to the list
      if (response != null) {
        responses.add(response);
      }
    }
    // do not redirect the channel message to the cluster to prevent infinite loops
    if (packet.uniqueId() != null) {
      responses.addAll(this.messenger
        .sendChannelMessageQueryAsync(message, comesFromWrapper)
        .get(20, TimeUnit.SECONDS, Collections.emptyList()));
      // respond with the available responses
      channel.sendPacket(packet.constructResponse(DataBuf.empty().writeObject(responses)));
    } else {
      this.messenger.sendChannelMessage(message, comesFromWrapper);
    }
    // force release the message
    message.content().enableReleasing().release();
  }
}
