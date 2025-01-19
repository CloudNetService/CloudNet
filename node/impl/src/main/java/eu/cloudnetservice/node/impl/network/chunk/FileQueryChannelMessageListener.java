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

package eu.cloudnetservice.node.impl.network.chunk;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.chunk.event.FileQueryRequestEvent;
import jakarta.inject.Inject;
import lombok.NonNull;

/**
 * A listener for channel messages that request a query transfer of a file.
 *
 * @since 4.0
 */
public final class FileQueryChannelMessageListener {

  private final EventManager eventManager;

  /**
   * Constructs a new query channel message listener instance.
   *
   * @param eventManager the event handler to use for posting request events.
   * @throws NullPointerException if the given event manager is null.
   */
  @Inject
  public FileQueryChannelMessageListener(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  /**
   * Constructs the response data for a request. The information contains the single information if the transfer
   * started.
   *
   * @param transferStarted true if a handler was found and the transfer was started, false otherwise.
   * @return the response data for the remote network component.
   */
  private static @NonNull DataBuf constructRequestResponse(boolean transferStarted) {
    return DataBufFactory.defaultFactory().createWithExpectedSize(1).writeBoolean(transferStarted);
  }

  /**
   * Handles chunked file query requests that come in via channel message.
   *
   * @param event the channel message receive event for any channel message.
   * @throws NullPointerException if the given event is null.
   */
  @EventListener
  public void handleFileQueryRequest(@NonNull ChannelMessageReceiveEvent event) {
    var channel = event.networkChannel();
    var request = event.channelMessage();
    if (!event.query()
      || !request.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)
      || !request.message().equals("chunked_query_file")) {
      return;
    }

    // read the request data & try to resolve a handler for the request
    var requestData = request.content();
    var chunkSize = requestData.readInt();
    var chunkedSessionId = requestData.readUniqueId();
    var requestedDataId = requestData.readString();

    var requestEvent = this.eventManager.callEvent(new FileQueryRequestEvent(requestedDataId, requestData));
    var responseHandlerBuilder = requestEvent.responseHandler();
    if (responseHandlerBuilder == null) {
      // no handler for the request provided
      var responseData = constructRequestResponse(false);
      event.binaryResponse(responseData);
    } else {
      // finish the response handler construct, start the transfer & respond with a success response
      responseHandlerBuilder
        .toChannels(channel)
        .chunkSize(chunkSize)
        .transferChannel("query:dummy")
        .sessionUniqueId(chunkedSessionId)
        .build()
        .transferChunkedData();
      var responseData = constructRequestResponse(true);
      event.binaryResponse(responseData);
    }
  }
}
