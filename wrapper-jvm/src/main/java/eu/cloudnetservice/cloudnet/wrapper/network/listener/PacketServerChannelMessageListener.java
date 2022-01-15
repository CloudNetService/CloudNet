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

package eu.cloudnetservice.cloudnet.wrapper.network.listener;

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.protocol.Packet;
import eu.cloudnetservice.cloudnet.driver.network.protocol.PacketListener;
import java.util.Collections;
import lombok.NonNull;

public final class PacketServerChannelMessageListener implements PacketListener {

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // skip the first boolean in the buffer as we don't need it
    packet.content().readBoolean();
    // read the channel message from the buffer
    var message = packet.content().readObject(ChannelMessage.class);
    // get the query response if available
    var response = CloudNetDriver.instance().eventManager().callEvent(
      new ChannelMessageReceiveEvent(message, channel, packet.uniqueId() != null)).queryResponse();
    // check if we need to respond to the channel message
    if (response != null || packet.uniqueId() != null) {
      // respond either using the query result or an empty result
      DataBuf content = response == null
        ? DataBuf.empty()
        : DataBuf.empty().writeObject(Collections.singleton(response));
      channel.sendPacket(packet.constructResponse(content));
    }
  }
}
