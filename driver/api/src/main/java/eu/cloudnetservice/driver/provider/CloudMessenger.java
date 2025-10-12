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

package eu.cloudnetservice.driver.provider;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main messaging api for communication in any form between components in the CloudNet cluster aside from sending
 * raw packets. The main difference between the raw packet api (network-component-based) and this api is that this api
 * searches the route to the target component rather than only accepting direct writes to a specific target component.
 * <p>
 * The target component search is only one layer deep, meaning that you can only send a channel message to another
 * component in the network known to the handling node, or its parent component (for services). Any other communication
 * form would break the normal CloudNet cluster structure. Channel messages can be sent to:
 * <ol>
 *   <li>Services: in this case, the handling node tries either to send the message directly to the service (if it is
 *   running on the local node) or via the node that is handling the service (which is connected to the handling node as
 *   required by the CloudNet cluster structure).
 *   <li>Nodes: in this case, the handling node sends the channel message directly to the connected node. This is
 *   possible as all nodes must be connected to all other nodes (as per the CloudNet cluster contract). This means that
 *   if (for example) Node-3 is only connected to Node-2 (which is connected to Node-1), and Node-1 receives a
 *   channel message for Node-3, the message cannot be routed to the target node.
 * </ol>
 *
 * @see ChannelMessage
 * @since 4.0
 */
public interface CloudMessenger {

  /**
   * The timeout (in milliseconds) that is applied to all sync query messaging methods. If no response is received
   * within the timespan, an exception is thrown by the method instead.
   */
  long SYNC_CHANNEL_MESSAGE_QUERY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);

  /**
   * Sends the given channel message to all of its targets. This method will not wait for the target component to
   * respond (it doesn't even expect a response) but for the handling component to send the message.
   * <p>
   * Note: once the channel message was sent, the backing buffer gets released. Therefore, the caller must acquire the
   * content buffer if the given channel message is sent multiple times.
   *
   * @param channelMessage the channel message to send.
   * @throws NullPointerException if the given channel message is null.
   */
  void sendChannelMessage(@NonNull ChannelMessage channelMessage);

  /**
   * Sends the given channel message as a query and blocks until all target components have responded or the timeout of
   * {@link #SYNC_CHANNEL_MESSAGE_QUERY_TIMEOUT_MS} is exceeded. If more control over the timeout is required, an async
   * method with a custom timeout applied must be used instead.
   * <p>
   * Note: it is not possible for CloudNet to detect when a channel message query response was consumed. Therefore, it
   * is crucial that the caller closes the responses to prevent memory leaks. Example:
   * <pre>
   * {@code
   * ChannelMessage message = ...;
   * Collection<ChannelMessage> responses = messenger.sendChannelMessageQuery(message);
   * for (var response : responses) {
   *   try (response) {
   *     // do something with the response
   *   }
   * }
   * }
   * </pre>
   *
   * @param channelMessage the channel message to send.
   * @return all responses of all components the given channel message is targeting.
   * @throws NullPointerException if the given channel message is null.
   * @throws CompletionException  if an exception occurred while waiting for the query responses.
   */
  @NonNull
  Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage);

  /**
   * Sends the given channel message as a query and blocks until one of the target component responded to the message or
   * the timeout of {@link #SYNC_CHANNEL_MESSAGE_QUERY_TIMEOUT_MS} is exceeded. If more control over the timeout is
   * required, an async method with a custom timeout applied must be used instead. This is in particular useful if there
   * is only one target, or you are only expecting one of the target components to respond.
   * <p>
   * Note: it is not possible for CloudNet to detect when a channel message query response was consumed. Therefore, it
   * is crucial that the caller closes the response to prevent memory leaks. Example:
   * <pre>
   * {@code
   * ChannelMessage message = ...;
   * ChannelMessage response = messenger.sendSingleChannelMessageQuery(message);
   * if (response != null) {
   *   try (response) {
   *     // do something with the response
   *   }
   * }
   * }
   * </pre>
   *
   * @param channelMessage the channel message to send.
   * @return the first response of any component the given message is targeting, null if no target responded.
   * @throws NullPointerException if the given channel message is null.
   * @throws CompletionException  if an exception occurred while waiting for the query response.
   */
  @Nullable
  ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage);

  /**
   * Sends the given channel message to all of its targets. This method will not wait for the target component to
   * respond (it doesn't even expect a response) but for the handling component to send the message.
   * <p>
   * Note: once the channel message was sent, the backing buffer gets released. Therefore, the caller must acquire the
   * content buffer if the given channel message is sent multiple times.
   *
   * @param channelMessage the channel message to send.
   * @return a future completed when the given channel message was sent.
   * @throws NullPointerException if the given channel message is null.
   */
  @NonNull
  CompletableFuture<Void> sendChannelMessageAsync(@NonNull ChannelMessage channelMessage);

  /**
   * Sends the given channel message as a query and returns a future which waits for target component(s) to respond. The
   * future will be completed when the target component responds or the query future times out.
   * <p>
   * Note: it is not possible for CloudNet to detect when a channel message query response was consumed. Therefore, it
   * is crucial that the caller closes the responses to prevent memory leaks. Example:
   * <pre>
   * {@code
   * ChannelMessage message = ...;
   * messenger.sendChannelMessageQueryAsync(message).thenAccept(responses -> {
   *   for (var response : responses) {
   *     try (response) {
   *       // do something with the response
   *     }
   *   }
   * }
   * }
   * </pre>
   *
   * @param message the channel message to send.
   * @return a future completed with all responses from all target network components.
   * @throws NullPointerException if the given channel message is null.
   */
  @NonNull
  CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NonNull ChannelMessage message);

  /**
   * Sends the given channel message as a query and returns a future which waits for target component(s) to respond.
   * Only the first response of any target will get sent back to this component. This is in particular useful if there
   * is only one target, or you are only expecting one of the target components to respond. The future will be completed
   * with the first received response of any target component (possibly null if no target responded).
   * <p>
   * Note: it is not possible for CloudNet to detect when a channel message query response was consumed. Therefore, it
   * is crucial that the caller closes the response to prevent memory leaks. Example:
   * <pre>
   * {@code
   * ChannelMessage message = ...;
   * messenger.sendSingleChannelMessageQueryAsync(message).thenAccept(response -> {
   *   if (response != null) {
   *     try (response) {
   *       // do something with the response
   *     }
   *   }
   * }
   * }
   * </pre>
   *
   * @param channelMessage the channel message to send.
   * @return a future completed with the first received response of any target component or null if no target responded.
   * @throws NullPointerException if the given channel message is null.
   */
  @NonNull
  CompletableFuture<ChannelMessage> sendSingleChannelMessageQueryAsync(@NonNull ChannelMessage channelMessage);
}
