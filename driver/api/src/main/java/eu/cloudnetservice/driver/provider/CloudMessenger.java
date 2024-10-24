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

package eu.cloudnetservice.driver.provider;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main messaging api for communication in any form between components in the CloudNet cluster aside from sending
 * raw packets. The main difference between the raw packet api (network component based) and this api is, that this
 * network point will search the route to the target component rather than only accepting direct writes to a specific
 * target component.
 * <p>
 * The target component search is only one layer deep, meaning that you can only send a channel message to another
 * component in the network known to the handling node, or its parent component (for services). Any other communication
 * form would break the normal CloudNet cluster structure. Channel messages can get send to:
 * <ol>
 *   <li>Services: in this case the handling node tries either to send the message directly to the service (if it is running
 *   on the local node) or via the parent node of the service (which must be connected!).
 *   <li>Nodes: in this case the handling node sends the channel message directly to the connected node. There is no
 *   second layer check, all nodes must be connected to all nodes (as per the CloudNet cluster contract). This means
 *   that if (for example) Node-3 is only connected to Node-2 (which is connected to Node-1), and Node-1 receives a
 *   channel message for Node-3 this will not work. This will work:
 *   <ol>
 *     <li>Node-1 gets a message for Node-2 (or the other way around).
 *     <li>Node-2 gets a message for Node-3 (or the other way around).
 *   </ol>
 * </ol>
 * <p>
 * A channel message received by a network component should always be acknowledged by the handling participant if it is
 * a query message to prevent possible deadlocks on the sender side.
 *
 * @see ChannelMessage
 * @since 4.0
 */
public interface CloudMessenger {

  /**
   * Sends the given channel message to all of its targets without waiting for a response from them.
   *
   * @param channelMessage the channel message to send.
   * @throws NullPointerException if the given channel message is null.
   */
  void sendChannelMessage(@NonNull ChannelMessage channelMessage);

  /**
   * Sends the given channel message to all of its targets and waits for all responses to be present or the query to
   * time out.
   *
   * @param channelMessage the channel message to send.
   * @return all responses from all network components which responded in time.
   * @throws NullPointerException if the given channel message is null.
   */
  @NonNull
  Collection<ChannelMessage> sendChannelMessageQuery(@NonNull ChannelMessage channelMessage);

  /**
   * Sends the given channel message to all of its targets and waits for all responses to be present or the query to
   * time out. This method will then peek the first response out of the returned array, or return null if no components
   * answered to the request.
   *
   * @param channelMessage the channel message to send.
   * @return the first response to the given channel message, can be null if no target responded.
   * @throws NullPointerException if the given channel message is null.
   */
  @Nullable
  ChannelMessage sendSingleChannelMessageQuery(@NonNull ChannelMessage channelMessage);

  /**
   * Sends the given channel message to all of its targets without waiting for a response from them.
   *
   * @param channelMessage the channel message to send.
   * @return a task completed when all channel messages were sent.
   * @throws NullPointerException if the given channel message is null.
   */
  @NonNull
  CompletableFuture<Void> sendChannelMessageAsync(@NonNull ChannelMessage channelMessage);

  /**
   * Sends the given channel message to all of its targets and waits for all responses to be present or the query to
   * time out.
   *
   * @param message the channel message to send.
   * @return a task completed with all responses from all network components which responded in time.
   * @throws NullPointerException if the given channel message is null.
   */
  @NonNull
  CompletableFuture<Collection<ChannelMessage>> sendChannelMessageQueryAsync(@NonNull ChannelMessage message);

  /**
   * Sends the given channel message to all of its targets and waits for all responses to be present or the query to
   * time out. This method will then peek the first response out of the returned array, or return null if no components
   * answered to the request.
   *
   * @param channelMessage the channel message to send.
   * @return a task completed with the first response to the given channel message, can be null if no target responded.
   * @throws NullPointerException if the given channel message is null.
   */
  @NonNull
  CompletableFuture<ChannelMessage> sendSingleChannelMessageQueryAsync(@NonNull ChannelMessage channelMessage);
}
