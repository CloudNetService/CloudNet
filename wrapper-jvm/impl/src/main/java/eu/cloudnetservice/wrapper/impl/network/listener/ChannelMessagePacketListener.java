/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.network.listener;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ChannelMessagePacketListener implements PacketListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelMessagePacketListener.class);

  private final EventManager eventManager;

  @Inject
  public ChannelMessagePacketListener(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    // for fields and order see ChannelMessagePacket
    var isQuery = packet.uniqueId() != null;
    var packetContent = packet.content();
    packetContent.readBoolean(); // skip comesFromWrapper info as it's not relevant
    var channelMessage = packet.content().readObject(ChannelMessage.class);

    // get the query response if available
    var event = this.eventManager.callEvent(new ChannelMessageReceiveEvent(channelMessage, channel, isQuery));
    var responseTask = event.queryResponse();
    if (isQuery) {
      // the wrapper has to always respond to channel message queries,
      // even if there is no response provided by any listener on the local service
      if (responseTask != null) {
        responseTask
          .whenComplete((_, _) -> channelMessage.content().release())
          .whenComplete((queryResponse, thrown) -> {
            this.sendChannelMessageResponse(queryResponse, packet, channel);
            if (thrown != null) {
              LOGGER.warn("Caught exception while constructing response to channel message {}", channelMessage, thrown);
            }
          });
        return;
      } else {
        this.sendChannelMessageResponse(null, packet, channel);
      }
    }

    channelMessage.content().release();
  }

  /**
   * Sends the given channel message response for a received channel message query.
   *
   * @param response      the response to send, null if no response was constructed.
   * @param requestPacket the packet that requested the channel message response.
   * @param sourceChannel the channel from which the query was received.
   * @throws NullPointerException if the given request packet or source channel is null.
   */
  private void sendChannelMessageResponse(
    @Nullable ChannelMessage response,
    @NonNull Packet requestPacket,
    @NonNull NetworkChannel sourceChannel
  ) {
    var responseData = response == null ? List.of() : List.of(response);
    var responseBuffer = DataBuf.empty().writeObject(responseData);
    var responsePacket = requestPacket.constructResponse(responseBuffer);
    sourceChannel.sendPacket(responsePacket);
  }
}
