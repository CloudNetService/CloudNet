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

package eu.cloudnetservice.node.impl.network.listener;

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.network.protocol.PacketListener;
import eu.cloudnetservice.node.impl.provider.NodeMessenger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ChannelMessagePacketListener implements PacketListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelMessagePacketListener.class);

  private final NodeMessenger messenger;
  private final EventManager eventManager;
  private final ComponentInfo componentInfo;

  @Inject
  public ChannelMessagePacketListener(
    @NonNull NodeMessenger messenger,
    @NonNull EventManager eventManager,
    @NonNull ComponentInfo componentInfo
  ) {
    this.messenger = messenger;
    this.eventManager = eventManager;
    this.componentInfo = componentInfo;
  }

  @Override
  public void handle(@NonNull NetworkChannel channel, @NonNull Packet packet) {
    var comesFromWrapper = packet.content().readBoolean();
    var message = packet.content().readObject(ChannelMessage.class);

    // check if we should handle the message locally
    var handleLocally = message.targets().stream().anyMatch(target -> switch (target.type()) {
      case ALL -> true;
      case NODE -> target.name() == null || target.name().equals(this.componentInfo.componentName());
      default -> false;
    });

    if (handleLocally) {
      // mark the index of the data buf & call the receive event
      message.content().acquire().startTransaction();
      // call the receive event
      var responseTask = this.eventManager
        .callEvent(new ChannelMessageReceiveEvent(message, channel, packet.uniqueId() != null))
        .queryResponse();
      // reset the index
      message.content().redoTransaction();

      // wait for the response to become available if given before resuming
      if (responseTask != null) {
        responseTask.thenAccept(response -> this.resumeHandling(packet, channel, message, response, comesFromWrapper));
        return;
      }
    }

    // resume instantly
    this.resumeHandling(packet, channel, message, null, comesFromWrapper);
  }

  private void resumeHandling(
    @NonNull Packet packet,
    @NonNull NetworkChannel channel,
    @NonNull ChannelMessage message,
    @Nullable ChannelMessage initialResponse,
    boolean comesFromWrapper
  ) {
    // do not redirect the channel message to the cluster to prevent infinite loops
    if (packet.uniqueId() != null) {
      this.messenger.sendChannelMessageQueryAsync(message, comesFromWrapper)
        .orTimeout(20, TimeUnit.SECONDS)
        .handle((result, exception) -> {
          // check if the handling was successful
          DataBuf responseContent;
          if (exception == null) {
            // respond with the result or just the single initial response if given
            if (result == null) {
              responseContent = initialResponse == null
                ? DataBuf.empty().writeBoolean(false)
                : DataBuf.empty().writeObject(Set.of(initialResponse));
            } else {
              // add the initial response if given before writing
              if (initialResponse != null) {
                result.add(initialResponse);
              }

              // serialize the response
              if (result.isEmpty()) {
                responseContent = DataBuf.empty().writeBoolean(false);
              } else {
                responseContent = DataBuf.empty().writeObject(result);
              }
            }
          } else {
            // just respond with nothing when an exception was thrown
            responseContent = DataBuf.empty().writeBoolean(false);
            LOGGER.error("Unable to relay channel message {} into cluster", message, exception);
          }

          // send the results to the sender
          channel.sendPacket(packet.constructResponse(responseContent));
          return null;
        }).whenComplete((_, exception) -> {
          // log any internal errors
          if (exception != null) {
            LOGGER.error("Unable to encode/send response to channel message {}", message, exception);
          }

          if (initialResponse != null) {
            initialResponse.content().release();
          }
        });
    } else {
      this.messenger.sendChannelMessage(message, comesFromWrapper);
      if (initialResponse != null) {
        initialResponse.content().release();
      }
    }

    // force release of the current message
    // this is an edge case that should not happen, but basically the handlers did not read
    // all the content from the buffer, or the buffer simply had no content
    // checking if the buffer was acquired only once ensures that no-one acquired the buffer
    // during the read process and wants to use the buffer later on
    var messageContent = message.content();
    if (messageContent.acquires() == 1) {
      messageContent.release();
    }
  }
}
