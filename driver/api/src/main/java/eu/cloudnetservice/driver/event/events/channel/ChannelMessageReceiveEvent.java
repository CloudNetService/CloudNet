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

package eu.cloudnetservice.driver.event.events.channel;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.event.events.network.NetworkEvent;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.provider.CloudMessenger;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An event called when a channel message of another component is received which targets the current network component.
 * In a node environment this event will not get called when the channel message is not targeting the current node,
 * event though the nodes receives the channel message for redirection into the cluster.
 * <p>
 * This event is especially useful as it removes the need to read channel messages by using an own created network
 * listener and gives the possibility to easily respond channel messages when needed.
 *
 * @see ChannelMessage
 * @see CloudMessenger
 * @since 4.0
 */
public final class ChannelMessageReceiveEvent extends NetworkEvent {

  private final ChannelMessage channelMessage;
  private final boolean query;

  private volatile CompletableFuture<ChannelMessage> queryResponse;

  /**
   * Constructs a new instance of a channel message receive event.
   *
   * @param message        the original message sent to the component.
   * @param networkChannel the channel from which the message came.
   * @param query          true if the channel message expects a response, false otherwise.
   * @throws NullPointerException if the given message or channel is null.
   */
  public ChannelMessageReceiveEvent(
    @NonNull ChannelMessage message,
    @NonNull NetworkChannel networkChannel,
    boolean query
  ) {
    super(networkChannel);

    this.channelMessage = message;
    this.query = query;
  }

  /**
   * Get the original sender of the channel message.
   * <p>
   * This call is equivalent to {@code channelMessage().sender()}.
   *
   * @return the original sender of the channel message.
   */
  public @NonNull ChannelMessageSender sender() {
    return this.channelMessage.sender();
  }

  /**
   * Get all targets of the received channel message.
   * <p>
   * This call is equivalent to {@code channelMessage().targets()}.
   *
   * @return all targets of the received channel message.
   */
  public @NonNull Collection<ChannelMessageTarget> targets() {
    return this.channelMessage.targets();
  }

  /**
   * Get the channel to which the channel message got sent. Should be unique and therefore a grouping target for
   * identification.
   * <p>
   * This call is equivalent to {@code channelMessage().channel()}.
   *
   * @return the channel to which the channel message got sent.
   */
  public @NonNull String channel() {
    return this.channelMessage.channel();
  }

  /**
   * Get the message key of the channel message. Should be unique and therefore the main target for identification.
   * <p>
   * This call is equivalent to {@code channelMessage().message()}.
   *
   * @return the message key of the channel message.
   */
  public @NonNull String message() {
    return this.channelMessage.message();
  }

  /**
   * Get the original channel message which was sent.
   *
   * @return the original channel message which was sent.
   */
  public @NonNull ChannelMessage channelMessage() {
    return this.channelMessage;
  }

  /**
   * Get the content of the channel message. The content will not get released when reading from it, however there is no
   * guarantee that a read from the buffer will succeed. A call to {@link DataBuf#redoTransaction()} will ensure that
   * the reader index of the buffer is reset to the first byte of the buffer (any read will start from the beginning
   * again then).
   * <p>
   * This call is equivalent to {@code channelMessage().content()}.
   *
   * @return the content of the channel message.
   */
  public @NonNull DataBuf content() {
    return this.channelMessage.content();
  }

  /**
   * Get if this channel message is a query. This means that the sending network components expects a response. But
   * there is no need for this component to respond to the channel message.
   *
   * @return true if the channel message expects a response, false otherwise.
   */
  public boolean query() {
    return this.query;
  }

  /**
   * Sets the response of this channel message to a channel message which just contains the given buffer as the content
   * of it.
   *
   * @param dataBuf the content of the channel message to respond with.
   * @throws NullPointerException     if the given content is null.
   * @throws IllegalArgumentException if the received channel message is not expecting a response.
   */
  public void binaryResponse(@NonNull DataBuf dataBuf) {
    this.queryResponse(ChannelMessage.buildResponseFor(this.channelMessage).buffer(dataBuf).build());
  }

  /**
   * Get the current query response to the received channel message. Null means, that there will be no response by this
   * component to the channel message.
   *
   * @return the current query response to the received channel message.
   */
  public @Nullable CompletableFuture<ChannelMessage> queryResponse() {
    return this.queryResponse;
  }

  /**
   * Sets the query response of the received channel message to the given channel message. Null is accepted and a valid
   * value, representing that there will be no response to the channel message.
   *
   * @param queryResponse the response to the received channel message.
   * @throws NullPointerException     if the given response is null.
   * @throws IllegalArgumentException if the received channel message is not expecting a response.
   */
  public void queryResponse(@Nullable ChannelMessage queryResponse) {
    Preconditions.checkArgument(this.query, "Cannot set query response of no query");
    this.queryResponse(queryResponse == null ? null : CompletableFuture.completedFuture(queryResponse));
  }

  /**
   * Sets the query response of the received channel message to the given channel message. Null is accepted and a valid
   * value, representing that there will be no response to the channel message. The caller of the event will wait for
   * the given task to compute a response but will resume the calling thread.
   *
   * @param queryResponse a future completed with the query response to the received channel message.
   * @throws NullPointerException     if the given response future is null.
   * @throws IllegalArgumentException if the received channel message is not expecting a response.
   */
  public void queryResponse(@Nullable CompletableFuture<ChannelMessage> queryResponse) {
    Preconditions.checkArgument(this.query, "Cannot set query response of no query");
    this.queryResponse = queryResponse;
  }
}
